package ru.nsu.ccfit.zuev.osu;

import android.graphics.PointF;
import android.util.Log;

import okio.BufferedSource;
import okio.Okio;

import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.GameObjectData;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;
import test.tpdifficulty.TimingPoint;
import test.tpdifficulty.hitobject.HitCircle;
import test.tpdifficulty.hitobject.HitObject;
import test.tpdifficulty.hitobject.HitObjectType;
import test.tpdifficulty.hitobject.Slider;
import test.tpdifficulty.hitobject.SliderType;
import test.tpdifficulty.hitobject.Spinner;
import test.tpdifficulty.tp.AiModtpDifficulty;

public class OSUParser {
    private final File file;
    private BufferedSource source;
    private boolean fileOpened = false;
    private final ArrayList<TimingPoint> timingPoints = new ArrayList<>();
    private final ArrayList<HitObject> hitObjects = new ArrayList<>();
    private TimingPoint currentTimingPoint = null;
    private BeatmapData data;
    private float sliderSpeed;
    private float sliderTick;

    public OSUParser(final File file) {
        this.file = file;
    }

    public OSUParser(final String path) {
        file = new File(path);
    }

    public boolean openFile() {
        try {
            source = Okio.buffer(Okio.source(file));
        } catch (final IOException e) {
            Debug.e("OSUParser.openFile: " + e.getMessage(), e);
            return false;
        }

        try {
            String head = source.readUtf8Line();
            Pattern pattern = Pattern.compile("osu file format v(\\d+)");
            Matcher matcher = pattern.matcher(head);
            if (!matcher.find()) {
                source.close();
                source = null;
                return false;
            }
        } catch (IOException e) {
            Debug.e("OSUParser.openFile: " + e.getMessage(), e);
        } catch (Exception e) {
            Debug.e("OSUParser.openFile: " + e.getMessage(), e);
        }

        fileOpened = true;
        return true;
    }

    public boolean readMetaData(final TrackInfo track, final BeatmapInfo info) {
        data = readData();

        if (!data.getData("General", "Mode").equals("0")
                && !data.getData("General", "Mode").equals("")) {
            return false;
        }

        for (BeatmapSection section : BeatmapSection.values()) {
            if (!loadMetadata(track, info, section)) {
                return false;
            }
        }

        // Calculate beatmap difficulty
        try {
            AiModtpDifficulty tpDifficulty = new AiModtpDifficulty();
            tpDifficulty.CalculateAll(this.hitObjects, track.getCircleSize());
            track.setDifficulty(GameHelper.Round(tpDifficulty.getStarRating(), 2));
        } catch (Exception e) {
            Debug.e("Beatmap <" + info.getPath() + "> has bad parameter, so give it up");
            return false;
        }

        // Debug.i("MaxCombo: " + track.getMaxCombo());
        // Debug.i("Caching " + track.getFilename());

        return true;
    }

    public int getMaxCombo() {
        int combo = 0;
        for (HitObject obj : hitObjects) {
            combo += obj.getCombo(sliderTick, sliderSpeed);
        }
        return combo;
    }

    public float tryParseFloat(String str, float defaultVal) {
        float val;
        try {
            val = Float.parseFloat(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }

    public int tryParseInt(String str, int defaultVal) {
        int val;
        try {
            val = Integer.parseInt(str);
        } catch (NumberFormatException ignored) {
            val = defaultVal;
        }
        return val;
    }

    public BeatmapData readData() {
        if (!fileOpened) {
            return null;
        }
        fileOpened = false;
        final BeatmapData data = new BeatmapData();

        String previousSection;
        String currentSection = null;

        // Events, TimingPoints, and HitObjects sections' data are stored in an instance
        // of ArrayList, while others are stored in an instance of HashMap
        final Map<String, String> map = new HashMap<>();
        final ArrayList<String> list = new ArrayList<>();

        try {
            while (true) {
                String s = source.readUtf8Line();

                // If s is null, it means we've reached the end of the file.
                // End the loop and don't forget to add the last section,
                // which would otherwise be ignored
                if (s == null) {
                    if (currentSection != null) {
                        switch (currentSection) {
                            case "Events":
                            case "TimingPoints":
                            case "HitObjects":
                                data.addSection(currentSection, list);
                                break;
                            default:
                                data.addSection(currentSection, map);
                        }
                    }
                    source.close();
                    break;
                }

                // Handle space comments
                if (s.startsWith(" ") || s.startsWith("_")) {
                    continue;
                }

                // Now that we've handled space comments, we can trim space
                s = s.trim();

                // Handle C++ style comments and empty lines
                if (s.startsWith("//") || s.isEmpty()) {
                    continue;
                }

                // [SectionName]
                if (s.startsWith("[")) {
                    // Record the current section before loading the new section.
                    // This is necessary so that we don't enter an empty data
                    // if we've just entered the first section (previousSection will
                    // be null)
                    previousSection = currentSection;
                    currentSection = s.substring(1, s.length() - 1);

                    if (previousSection == null) {
                        continue;
                    }

                    // Enter a deep copy of the original data holder so that
                    // we can use the original for the next section
                    switch (previousSection) {
                        case "Events":
                        case "TimingPoints":
                        case "HitObjects":
                            data.addSection(previousSection, new ArrayList<>(list));
                            list.clear();
                            break;
                        default:
                            data.addSection(previousSection, new HashMap<>(map));
                            map.clear();
                    }
                    continue;
                }

                // If we're still not in a section yet, there is no need
                // to start parsing data, just continue to the next line
                if (currentSection == null) {
                    continue;
                }

                // Collect and parse data depending on section
                switch (currentSection) {
                    case "Events":
                    case "TimingPoints":
                    case "HitObjects":
                        list.add(s);
                        break;
                    default:
                        final String[] pair = s.split("\\s*:\\s*", 2);
                        if (pair.length > 1) {
                            map.put(pair[0], pair[1]);
                        }
                }
            }
        } catch (IOException e) {
            Debug.e("OSUParser.readData: " + e.getMessage(), e);
            return data;
        }

        data.setFolder(file.getParent());

        return data;
    }

    private enum BeatmapSection {
        GENERAL,
        EDITOR,
        METADATA,
        DIFFICULTY,
        EVENTS,
        TIMINGPOINTS,
        HITOBJECTS
    }

    private boolean loadMetadata(final TrackInfo track, final BeatmapInfo info, final BeatmapSection section) {
        switch (section) {
            case GENERAL:
                return loadGeneralSection(info);
            case METADATA:
                return loadMetadataSection(track, info);
            case DIFFICULTY:
                return loadDifficultySection(track);
            case EVENTS:
                return loadEventsSection(track);
            case TIMINGPOINTS:
                return loadTimingPointsSection(track);
            case HITOBJECTS:
                return loadHitObjectsSection(track);
            default:
                return true;
        }
    }

    private boolean loadGeneralSection(final BeatmapInfo info) {
        if (info.getTitle() == null) {
            info.setTitle(data.getData("Metadata", "Title"));
        }
        if (info.getTitleUnicode() == null) {
            String titleUnicode = data.getData("Metadata", "TitleUnicode");
            if (titleUnicode != null && titleUnicode.length() > 0) {
                info.setTitleUnicode(titleUnicode);
            }
        }
        if (info.getArtist() == null) {
            info.setArtist(data.getData("Metadata", "Artist"));
        }
        if (info.getArtistUnicode() == null) {
            String artistUnicode = data.getData("Metadata", "ArtistUnicode");
            if (artistUnicode != null && artistUnicode.length() > 0) {
                info.setArtistUnicode(artistUnicode);
            }
        }
        if (info.getSource() == null) {
            info.setSource(data.getData("Metadata", "Source"));
        }
        if (info.getTags() == null) {
            info.setTags(data.getData("Metadata", "Tags"));
        }

        if (info.getMusic() == null) {
            final File musicFile = new File(info.getPath(), data.getData(
                    "General", "AudioFilename"));
            if (!musicFile.exists()) {
                ToastLogger.showText(StringTable.format(R.string.osu_parser_music_not_found,
                        file.getName().substring(0, file.getName().length() - 4)), true);
                return false;
            }
            info.setMusic(musicFile.getPath());
            final String prevTime = data.getData("General", "PreviewTime");
            try {
                info.setPreviewTime(Integer.parseInt(prevTime));
            } catch (final NumberFormatException e) {
                Debug.e("Cannot parse preview time");
                info.setPreviewTime(-1);
            }
        }

        return true;
    }

    private boolean loadMetadataSection(final TrackInfo track, final BeatmapInfo info) {
        track.setCreator(data.getData("Metadata", "Creator"));
        track.setMode(data.getData("Metadata", "Version"));

        track.setPublicName(info.getArtist() + " - " + info.getTitle());

        track.setBeatmapID(tryParseInt(data.getData("Metadata", "BeatmapID"), -1));
        track.setBeatmapSetID(tryParseInt(data.getData("Metadata", "BeatmapSetID"), -1));

        return true;
    }

    private boolean loadDifficultySection(final TrackInfo track) {
        track.setOverallDifficulty(tryParseFloat(data.getData("Difficulty", "OverallDifficulty"), 5f));
        track.setApproachRate(tryParseFloat(data.getData("Difficulty", "ApproachRate"), track.getOverallDifficulty()));
        track.setHpDrain(tryParseFloat(data.getData("Difficulty", "HPDrainRate"), 5f));
        track.setCircleSize(tryParseFloat(data.getData("Difficulty", "CircleSize"), 4f));

        sliderTick = tryParseFloat(data.getData("Difficulty", "SliderTickRate"), 1.0f);
        sliderSpeed = tryParseFloat(data.getData("Difficulty", "SliderMultiplier"), 1.0f);

        return true;
    }

    private boolean loadEventsSection(final TrackInfo track) {
        // We only need to load beatmap background
        for (final String s : data.getData("Events")) {
            final String[] pars = s.split("\\s*,\\s*");
            if (pars.length >= 3 && s.startsWith("0,0")) {
                track.setBackground(pars[2].substring(1, pars[2].length() - 1));
                break;
            }
        }

        return true;
    }

    private boolean loadTimingPointsSection(final TrackInfo track) {
        timingPoints.clear();

        // Get the first uninherited timing point
        for (final String tempString : data.getData("TimingPoints")) {
            String[] rawData = tempString.split("[,]");
            // Handle malformed timing point
            if (rawData.length < 2) {
                return false;
            }
            float bpm = tryParseFloat(rawData[1], Float.NaN);
            if (Float.isNaN(bpm)) {
                return false;
            }

            // Uninherited: bpm > 0
            if (bpm > 0) {
                float offset = tryParseFloat(rawData[0], Float.NaN);
                if (Float.isNaN(offset)) {
                    return false;
                }
                currentTimingPoint = new TimingPoint(bpm, offset, 1f);
                break;
            }
        }

        if (currentTimingPoint == null) {
            ToastLogger.showText(StringTable.format(R.string.osu_parser_timing_error,
                    file.getName().substring(0, file.getName().length() - 4)), true);
            return false;
        }

        for (final String tempString : data.getData("TimingPoints")) {
            String[] rawData = tempString.split("[,]");
            // Handle malformed timing point
            if (rawData.length < 2) {
                return false;
            }
            float offset = tryParseFloat(rawData[0], Float.NaN);
            float bpm = tryParseFloat(rawData[1], Float.NaN);
            if (Float.isNaN(offset) || Float.isNaN(bpm)) {
                return false;
            }
            float speed = 1.0f;
            boolean inherited = bpm < 0;

            if (inherited) {
                speed = -100.0f / bpm;
                bpm = currentTimingPoint.getBpm();
            } else {
                bpm = 60000.0f / bpm;
            }
            TimingPoint timing = new TimingPoint(bpm, offset, speed);
            if (!inherited) {
                currentTimingPoint = timing;
            }
            try {
                bpm = GameHelper.Round(bpm, 2);
            } catch (NumberFormatException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
                Log.e("Beatmap Error", "" + track.getMode());
                ToastLogger.showText(StringTable.get(R.string.osu_parser_error) + " " + track.getMode(), true);
                return false;
            }
            track.setBpmMin(track.getBpmMin() != Float.MAX_VALUE ? Math.min(track.getBpmMin(), bpm) : bpm);
            track.setBpmMax(track.getBpmMax() != 0 ? Math.max(track.getBpmMax(), bpm) : bpm);
            timingPoints.add(timing);
        }

        return timingPoints.size() > 0;
    }

    private boolean loadHitObjectsSection(final TrackInfo track) {
        final ArrayList<String> hitObjects = data.getData("HitObjects");
        if (hitObjects.size() <= 0) {
            return false;
        }

        track.setTotalHitObjectCount(hitObjects.size());

        this.hitObjects.clear();
        int tpIndex = 0;
        currentTimingPoint = timingPoints.get(tpIndex);

        for (final String tempString : hitObjects) {
            String[] hitObjectData = tempString.split("[,]");
            String[] rawData;

            // Ignoring v10 features
            int dataSize = hitObjectData.length;
            while (dataSize > 0 && hitObjectData[dataSize - 1].matches("([0-9][:][0-9][|]?)+")) {
                dataSize--;
            }
            if (dataSize < hitObjectData.length) {
                rawData = new String[dataSize];
                for (int i = 0; i < rawData.length; i++) {
                    rawData[i] = hitObjectData[i];
                }
            } else {
                rawData = hitObjectData;
            }

            // Handle malformed hitobject
            if (rawData.length < 4) {
                return false;
            }

            int time = tryParseInt(rawData[2], -1);
            if (time <= -1) {
                return false;
            }
            while (tpIndex < timingPoints.size() - 1 && timingPoints.get(tpIndex + 1).getOffset() <= time) {
                tpIndex++;
            }
            currentTimingPoint = timingPoints.get(tpIndex);
            HitObjectType hitObjectType = HitObjectType.valueOf(tryParseInt(rawData[3], -1) % 16);
            PointF pos = new PointF(
                tryParseFloat(rawData[0], Float.NaN),
                tryParseFloat(rawData[1], Float.NaN)
            );
            if (Float.isNaN(pos.x) || Float.isNaN(pos.y)) {
                return false;
            }
            HitObject object = null;
            if (hitObjectType == null) {
                System.out.println(tempString);
                return false;
            }

            if (hitObjectType == HitObjectType.Normal || hitObjectType == HitObjectType.NormalNewCombo) {
                // HitCircle
                object = new HitCircle(time, pos, currentTimingPoint);
                track.setHitCircleCount(track.getHitCircleCount() + 1);
            } else if (hitObjectType == HitObjectType.Spinner) {
                // Spinner
                int endTime = tryParseInt(rawData[5], -1);
                if (endTime <= -1) {
                    return false;
                }
                object = new Spinner(time, endTime, pos, currentTimingPoint);
                track.setSpinnerCount(track.getSpinnerCount() + 1);
            } else if (hitObjectType == HitObjectType.Slider || hitObjectType == HitObjectType.SliderNewCombo) {
                // Slider
                // Handle malformed slider
                boolean isValidSlider = rawData.length >= 8;
                if (!isValidSlider) {
                    return false;
                }

                String[] curvePointsData = rawData[5].split("[|]");
                SliderType sliderType = SliderType.parse(curvePointsData[0].charAt(0));
                ArrayList<PointF> curvePoints = new ArrayList<>();
                for (int i = 1; i < curvePointsData.length; i++) {
                    String[] curvePointData = curvePointsData[i].split("[:]");
                    PointF curvePointPosition = new PointF(
                        tryParseFloat(curvePointData[0], Float.NaN),
                        tryParseFloat(curvePointData[1], Float.NaN)
                    );

                    if (Float.isNaN(curvePointPosition.x) || Float.isNaN(curvePointPosition.y)) {
                        isValidSlider = false;
                        break;
                    }
                    curvePoints.add(curvePointPosition);
                }
                if (!isValidSlider) {
                    return false;
                }

                int repeat = tryParseInt(rawData[6], -1);
                float rawLength = tryParseFloat(rawData[7], Float.NaN);
                if (repeat <= -1 || Float.isNaN(rawLength)) {
                    continue;
                }

                int endTime = time + (int) (rawLength * (600 / timingPoints.get(0).getBpm()) / sliderSpeed) * repeat;
                object = new Slider(time, endTime, pos, currentTimingPoint, sliderType, repeat, curvePoints, rawLength);
                track.setSliderCount(track.getSliderCount() + 1);
            }
            this.hitObjects.add(object);
        }

        int length = (int) tryParseFloat(new GameObjectData(hitObjects.get(hitObjects.size() - 1)).getData()[2], 0);
        track.setMusicLength(length);
        track.setMaxCombo(getMaxCombo());

        return this.hitObjects.size() > 0;
    }
}
