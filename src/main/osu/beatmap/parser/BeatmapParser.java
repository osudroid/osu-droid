package main.osu.beatmap.parser;

import android.util.Log;

import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
import com.rimu.R;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.osu.BeatmapInfo;
import main.osu.ToastLogger;
import main.osu.TrackInfo;
import main.osu.Utils;
import main.osu.beatmap.BeatmapData;
import main.osu.beatmap.constants.BeatmapSection;
import main.osu.beatmap.parser.sections.BeatmapColorParser;
import main.osu.beatmap.parser.sections.BeatmapControlPointsParser;
import main.osu.beatmap.parser.sections.BeatmapDifficultyParser;
import main.osu.beatmap.parser.sections.BeatmapEventsParser;
import main.osu.beatmap.parser.sections.BeatmapGeneralParser;
import main.osu.beatmap.parser.sections.BeatmapHitObjectsParser;
import main.osu.beatmap.parser.sections.BeatmapMetadataParser;
import main.osu.helper.StringTable;
import okio.BufferedSource;
import okio.Okio;

/**
 * A parser for parsing <code>.osu</code> files.
 */
public class BeatmapParser {
    /**
     * The <code>.osu</code> file.
     */
    private final File file;

    /**
     * The <code>BufferedSource</code> responsible for reading the beatmap file's contents.
     */
    private BufferedSource source;

    /**
     * @param file The <code>.osu</code> file.
     */
    public BeatmapParser(final File file) {
        this.file = file;
    }

    /**
     * @param path The path to the <code>.osu</code> file.
     */
    public BeatmapParser(final String path) {
        file = new File(path);
    }

    /**
     * Attempts to open the beatmap file.
     *
     * @return Whether the beatmap file was successfully opened.
     */
    public boolean openFile() {
        try {
            source = Okio.buffer(Okio.source(file));
        } catch (final IOException e) {
            Log.e("BeatmapParser.openFile", e.getMessage());
            source = null;
            return false;
        }

        try {
            String head = source.readUtf8Line();
            if (head == null) {
                source.close();
                source = null;
                return false;
            }

            Pattern pattern = Pattern.compile("osu file format v(\\d+)");
            Matcher matcher = pattern.matcher(head);
            if (!matcher.find()) {
                source.close();
                source = null;
                return false;
            }
        } catch (Exception e) {
            Log.e("BeatmapParser.openFile", e.getMessage());
        }

        return true;
    }

    /**
     * Parses the <code>.osu</code> file.
     *
     * @param withHitObjects Whether to also parse hit objects.
     * @return A <code>BeatmapData</code> containing relevant information of the beatmap file,
     * <code>null</code> if the beatmap file cannot be opened or a line could not be parsed.
     */
    public BeatmapData parse(boolean withHitObjects) {
        if (source == null && !openFile()) {
            return null;
        }

        BeatmapSection currentSection = null;
        BeatmapData data = new BeatmapData();
        data.setFolder(file.getParent());

        BeatmapGeneralParser generalParser = new BeatmapGeneralParser();
        BeatmapMetadataParser metadataParser = new BeatmapMetadataParser();
        BeatmapDifficultyParser difficultyParser = new BeatmapDifficultyParser();
        BeatmapEventsParser eventsParser = new BeatmapEventsParser();
        BeatmapControlPointsParser controlPointsParser = new BeatmapControlPointsParser();
        BeatmapColorParser colorParser = new BeatmapColorParser();
        BeatmapHitObjectsParser hitObjectsParser = new BeatmapHitObjectsParser(withHitObjects);

        String s;

        try {
            while ((s = source.readUtf8Line()) != null) {
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
                if (s.startsWith("[") && s.endsWith("]")) {
                    currentSection = BeatmapSection.parse(s.substring(1, s.length() - 1));
                    continue;
                }

                // Beatmap format version
                int formatPos = s.indexOf("file format v");
                if (formatPos >= 0) {
                    data.setFormatVersion(Utils.tryParseInt(s.substring(formatPos + 13), data.getFormatVersion()));
                }

                if (currentSection == null) {
                    continue;
                }

                boolean successfulParse = true;
                switch (currentSection) {
                    case general:
                        successfulParse = generalParser.parse(data, s);
                        break;
                    case metadata:
                        successfulParse = metadataParser.parse(data, s);
                        break;
                    case difficulty:
                        successfulParse = difficultyParser.parse(data, s);
                        break;
                    case events:
                        successfulParse = eventsParser.parse(data, s);
                        break;
                    case timingPoints:
                        successfulParse = controlPointsParser.parse(data, s);
                        break;
                    case colors:
                        successfulParse = colorParser.parse(data, s);
                        break;
                    case hitObjects:
                        successfulParse = hitObjectsParser.parse(data, s);
                        break;
                }

                if (!successfulParse) {
                    Log.e("BeatmapParser.parse", "Unable to parse line " + s);
                    return null;
                }
            }
        } catch (IOException e) {
            Log.e("BeatmapParser.parse", e.getMessage());
            return data;
        }

        return data;
    }

    /**
     * Given a <code>TrackInfo</code>, populate its metadata with a <code>BeatmapData</code>.
     *
     * @param data The <code>BeatmapData</code> to populate the <code>TrackInfo</code> with.
     * @param info The <code>TrackInfo</code> to populate.
     *
     * @return Whether the given <code>TrackInfo</code> was successfully populated.
     */
    public boolean populateMetadata(final BeatmapData data, final TrackInfo info) {
        // General
        final File musicFile = new File(data.getFolder(), data.general.audioFilename);
        if (!musicFile.exists()) {
            ToastLogger.showText(StringTable.format(R.string.osu_parser_music_not_found,
                    file.getName().substring(0, file.getName().length() - 4)), true);
            return false;
        }

        info.setMusic(musicFile.getPath());
        info.setPreviewTime(data.general.previewTime);

        // Metadata
        info.setCreator(data.metadata.creator);
        info.setMode(data.metadata.version);
        info.setPublicName(data.metadata.artist + " - " + data.metadata.title);
        info.setBeatmapID(data.metadata.beatmapID);
        info.setBeatmapSetID(data.metadata.beatmapSetID);

        // Difficulty
        info.setOverallDifficulty(data.difficulty.od);
        info.setApproachRate(data.difficulty.ar);
        info.setHpDrain(data.difficulty.hp);
        info.setCircleSize(data.difficulty.cs);

        // Events
        info.setBackground(data.events.backgroundFilename);

        // Timing points
        for (TimingControlPoint point : data.timingPoints.timing.getControlPoints()) {
            float bpm = (float) point.getBPM();

            info.setBpmMin(info.getBpmMin() != Float.MAX_VALUE ? Math.min(info.getBpmMin(), bpm) : bpm);
            info.setBpmMax(info.getBpmMax() != 0 ? Math.max(info.getBpmMax(), bpm) : bpm);
        }

        // Hit objects
        info.setTotalHitObjectCount(data.hitObjects.getObjects().size());
        info.setHitCircleCount(data.hitObjects.getCircleCount());
        info.setSliderCount(data.hitObjects.getSliderCount());
        info.setSpinnerCount(data.hitObjects.getSpinnerCount());

        HitObject lastObject = data.hitObjects.getObjects().get(data.hitObjects.getObjects().size() - 1);

        info.setMusicLength((int) lastObject.getStartTime());
        if (lastObject instanceof HitObjectWithDuration) {
            info.setMusicLength((int) ((HitObjectWithDuration) lastObject).getEndTime());
        }
        info.setMaxCombo(data.getMaxCombo());

        return true;
    }

    /**
     * Given a <code>BeatmapInfo</code>, populate its metadata with a <code>BeatmapData</code>.
     *
     * @param data The <code>BeatmapData</code> to populate the <code>BeatmapInfo</code> with.
     * @param info The <code>BeatmapInfo</code> to populate.
     *
     * @return Whether the given <code>BeatmapInfo</code> was successfully populated.
     */
    public boolean populateMetadata(final BeatmapData data, final BeatmapInfo info) {
        // General
        if (info.getMusic() == null) {
            final File musicFile = new File(info.getPath(), data.general.audioFilename);
            if (!musicFile.exists()) {
                ToastLogger.showText(StringTable.format(R.string.osu_parser_music_not_found,
                        file.getName().substring(0, file.getName().length() - 4)), true);
                return false;
            }
            info.setMusic(musicFile.getPath());
            info.setPreviewTime(data.general.previewTime);
        }

        // Metadata
        if (info.getTitle() == null) {
            info.setTitle(data.metadata.title);
        }
        if (info.getTitleUnicode() == null) {
            String titleUnicode = data.metadata.titleUnicode;
            if (!titleUnicode.isEmpty()) {
                info.setTitleUnicode(titleUnicode);
            }
        }
        if (info.getArtist() == null) {
            info.setArtist(data.metadata.artist);
        }
        if (info.getArtistUnicode() == null) {
            String artistUnicode = data.metadata.artist;
            if (!artistUnicode.isEmpty()) {
                info.setArtistUnicode(artistUnicode);
            }
        }
        if (info.getSource() == null) {
            info.setSource(data.metadata.source);
        }
        if (info.getTags() == null) {
            info.setTags(data.metadata.tags);
        }

        return true;
    }
}
