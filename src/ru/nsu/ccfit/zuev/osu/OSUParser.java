package ru.nsu.ccfit.zuev.osu;

import android.graphics.PointF;
import android.util.Log;

import org.anddev.andengine.util.Debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    private BufferedReader reader = null;
    private boolean fileOpened = false;
    private ArrayList<TimingPoint> timingPoints;
    private ArrayList<HitObject> hitObjects;
    private TimingPoint currentTimingPoint;
    private int tpIndex = 0;
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
            FileReader fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
        } catch (final FileNotFoundException e) {
            Debug.e("OSUParser.openFile: " + e.getMessage(), e);
            return false;
        }

        String head;
        try {
            head = reader.readLine().trim();
            Pattern pattern = Pattern.compile("osu file format v(\\d+)");
            Matcher matcher = pattern.matcher(head);
            if (!matcher.find()) {
                reader.close();
                reader = null;
                return false;
            }
        } catch (IOException e) {
            Debug.e("OSUParser.openFile: " + e.getMessage(), e);
        } catch (NullPointerException e) {
            Debug.e("OSUParser.openFile: " + e.getMessage(), e);
        }

        fileOpened = true;
        return true;
    }

    public boolean readMetaData(final TrackInfo track, final BeatmapInfo info) {
        final BeatmapData data = readData();

        if (data.getData("General", "Mode").equals("0") == false
                && data.getData("General", "Mode").equals("") == false) {
            return false;
        }

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
        track.setCreator(data.getData("Metadata", "Creator"));
        track.setMode(data.getData("Metadata", "Version"));

        track.setPublicName(info.getArtist() + " - " + info.getTitle());

        track.setBeatmapID(tryParseInt(data.getData("Metadata", "BeatmapID"), -1));
        track.setBeatmapSetID(tryParseInt(data.getData("Metadata", "BeatmapSetID"), -1));

        track.setOverallDifficulty(tryParse(data.getData("Difficulty", "OverallDifficulty"), 5f));
        track.setApproachRate(tryParse(data.getData("Difficulty", "ApproachRate"), track.getOverallDifficulty()));
        track.setHpDrain(tryParse(data.getData("Difficulty", "HPDrainRate"), 5f));
        track.setCircleSize(tryParse(data.getData("Difficulty", "CircleSize"), 4f));

        this.sliderTick = tryParse(data.getData("Difficulty", "SliderTickRate"), 1.0f);
        this.sliderSpeed = tryParse(data.getData("Difficulty", "SliderMultiplier"), 1.0f);

        if (info.getMusic() == null) {
            final File musicFile = new File(info.getPath(), data.getData(
                    "General", "AudioFilename"));
            if (musicFile.exists()) {
                info.setMusic(musicFile.getPath());
            }
            final String prevTime = data.getData("General", "PreviewTime");
            try {
                info.setPreviewTime(Integer.parseInt(prevTime));
            } catch (final NumberFormatException e) {
                Debug.e("Cannot parse preview time");
                info.setPreviewTime(-1);
            }
        }

        float breakTime = 0;
        for (final String s : data.getData("Events")) {
            final String[] pars = s.split("\\s*,\\s*");
            if (pars.length >= 3 && pars[0].equals("0") && pars[1].equals("0")) {
                track.setBackground(pars[2].substring(1, pars[2].length() - 1));
            } else if (pars.length >= 3 && pars[0].equals("2")) {
                breakTime += Float.parseFloat(pars[2])
                        - Float.parseFloat(pars[1]);
            }
        }
        //get first no inherited timingpoint
        for (final String tempString : data.getData("TimingPoints")) {
            String[] tmpdata = tempString.split("[,]");
            if(Float.parseFloat(tmpdata[1]) > 0){
                float offset = Float.parseFloat(tmpdata[0]);
                float bpm = 60000.0f / Float.parseFloat(tmpdata[1]);
                float speed = 1.0f;
                TimingPoint timing = new TimingPoint(bpm, offset, speed);
                currentTimingPoint = timing;
                break;
            }
        }
        //load all timingpoint
        for (final String tempString : data.getData("TimingPoints")) {
            if (timingPoints == null) {
                timingPoints = new ArrayList<TimingPoint>();
            }
            String[] tmpdata = tempString.split("[,]");
            float offset = Float.parseFloat(tmpdata[0]);
            float bpm = Float.parseFloat(tmpdata[1]);
            float speed = 1.0f;
            boolean inherited = false;
            if (bpm < 0) {
                inherited = true;
                speed = -100.0f / bpm;
                bpm = currentTimingPoint.getBpm();
            } else {
                bpm = 60000.0f / bpm;
            }
            TimingPoint timing = new TimingPoint(bpm, offset, speed);
            if (!inherited) currentTimingPoint = timing;
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
        final ArrayList<String> hitObjectss = data.getData("HitObjects");
        if (hitObjectss.size() <= 0) {
            return false;
        }
        track.setTotalHitObjectCount(hitObjectss.size());
        for (final String tempString : hitObjectss) {
            if (hitObjects == null) {
                hitObjects = new ArrayList<HitObject>();
                tpIndex = 0;
                currentTimingPoint = timingPoints.get(tpIndex);
            }
            String[] data1 = tempString.split("[,]");
            String[] rawdata = null;
            //Ignoring v10 features
            int dataSize = data1.length;
            while (dataSize > 0 && data1[dataSize - 1].matches("([0-9][:][0-9][|]?)+")) {
                dataSize--;
            }
            if (dataSize < data1.length) {
                rawdata = new String[dataSize];
                for (int i = 0; i < rawdata.length; i++) {
                    rawdata[i] = data1[i];
                }
            } else
                rawdata = data1;

            int time = Integer.parseInt(rawdata[2]);
            while (tpIndex < timingPoints.size() - 1 && timingPoints.get(tpIndex + 1).getOffset() <= time) {
                tpIndex += 1;
            }
            currentTimingPoint = timingPoints.get(tpIndex);
            HitObjectType hitObjectType = HitObjectType.valueOf(Integer.parseInt(rawdata[3]) % 16);
            PointF pos = new PointF(Float.parseFloat(rawdata[0]), Float.parseFloat(rawdata[1]));
            HitObject object = null;
            if (hitObjectType == null) {
                System.out.println(tempString);
                continue;
            }
            if (hitObjectType == HitObjectType.Normal || hitObjectType == HitObjectType.NormalNewCombo) { // hitcircle
                object = new HitCircle(time, pos, currentTimingPoint);
                track.setHitCircleCount(track.getHitCircleCount() + 1);
            } else if (hitObjectType == HitObjectType.Spinner) { // spinner
                int endTime = Integer.parseInt(rawdata[5]);
                object = new Spinner(time, endTime, pos, currentTimingPoint);
                track.setSpinerCount(track.getSpinerCount() + 1);
            } else if (hitObjectType == HitObjectType.Slider || hitObjectType == HitObjectType.SliderNewCombo) { // slider
                String data2[] = rawdata[5].split("[|]");
                SliderType sliderType = SliderType.parse(data2[0].charAt(0));
                ArrayList<PointF> poss = new ArrayList<PointF>();
                for (int i = 1; i < data2.length; i++) {
                    String temp[] = data2[i].split("[:]");
                    poss.add(new PointF(Float.parseFloat(temp[0]), Float.parseFloat(temp[1])));
                }
                int repeat = Integer.parseInt(rawdata[6]);
                float rawLength = Float.parseFloat(rawdata[7]);
                int endTime = time + (int) (rawLength * (600 / timingPoints.get(0).getBpm()) / sliderSpeed) * repeat;
                object = new Slider(time, endTime, pos, currentTimingPoint, sliderType, repeat, poss, rawLength);
                track.setSliderCount(track.getSliderCount() + 1);
            }
            hitObjects.add(object);
        }

        int length = (int) tryParse(new GameObjectData(hitObjectss.get(hitObjectss.size() - 1)).getData()[2], 0);
        track.setMusicLength(length);
        try {
            AiModtpDifficulty tpDifficulty = new AiModtpDifficulty();
            tpDifficulty.CalculateAll(hitObjects, track.getCircleSize());
            track.setDifficulty(GameHelper.Round(tpDifficulty.getStarRating(), 2));
        } catch (Exception e) {
            Debug.e("Beatmap <" + info.getPath() + "> has bad parameter, so give it up");
            return false;
        }
        track.setMaxCombo(getMaxCombo());
        Debug.i("MaxCombo: " + track.getMaxCombo());

        Debug.i("Caching " + track.getFilename());

//		track.setDifficulty(getStars(data, breakTime));
//		Debug.i(track.toString());
        return true;
    }

    public int getMaxCombo() {
        int combo = 0;
        for (HitObject obj : hitObjects) {
            combo += obj.getCombo(sliderTick, sliderSpeed);
        }
        return combo;
    }

    public float tryParse(String str, float defaultVal) {
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
        if (fileOpened == false) {
            return null;
        }
        fileOpened = false;
        String sname;
        final BeatmapData data = new BeatmapData();

        while (true) {
            sname = getNextSectionName();
            if (sname == null) {
                break;
            }

            if (sname.equals("Events") || sname.equals("TimingPoints")
                    || sname.equals("HitObjects")) {
                data.addSection(sname, parseDataSection());
            } else {
                data.addSection(sname, parseSection());
            }
        }

        data.setFolder(file.getParent());

        return data;
    }

    private String getNextSectionName() {
        String s;
        try {
            while ((s = reader.readLine()) != null) {
                if (s.matches("\\[\\S+\\]")) {
                    return s.substring(1, s.length() - 1);
                }
            }
        } catch (IOException e) {
            Debug.e("OSUParser.getNextSectionName: " + e.getMessage(), e);
        }
        return null;
    }

    private Map<String, String> parseSection() {
        final Map<String, String> map = new HashMap<String, String>();

        String s;
        try {
            while ((s = reader.readLine()) != null) {
                if (s.matches(".+:.*") == false) {
                    if (s.matches("//.+")) {
                        continue;
                    } else {
                        if (!s.isEmpty()) {
                            reader.reset();
                        }
                        break;
                    }
                }
                reader.mark(1);
                final String[] pair = s.split("\\s*:\\s*", 2);

                if (pair.length > 1) {
                    map.put(pair[0], pair[1]);
                }
            }
        } catch (IOException e) {
            Debug.e("OSUParser.parseSection: " + e.getMessage(), e);
        }

        return map;
    }

    private ArrayList<String> parseDataSection() {
        final ArrayList<String> list = new ArrayList<String>();

        String s;
        try {
            while ((s = reader.readLine()) != null) {
                if (s.matches("[^\\[].+") == false) {
                    if (s.matches("//.+")) {
                        continue;
                    } else {
                        if (!s.isEmpty()) {
                            reader.reset();
                        }
                        break;
                    }
                }
                reader.mark(1);
                list.add(s);
            }
        } catch (IOException e) {
            Debug.e("OSUParser.parseDataSection: " + e.getMessage(), e);
        }

        return list;
    }
}
