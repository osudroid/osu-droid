package ru.nsu.ccfit.zuev.osu.beatmap.parser;

import android.util.Log;

import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.utils.HitObjectStackEvaluator;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okio.BufferedSource;
import okio.Okio;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapSection;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapColorParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapControlPointsParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapDifficultyParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapEventsParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapGeneralParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapHitObjectsParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapMetadataParser;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

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
     * The format version of the beatmap.
     */
    private int beatmapFormatVersion = 14;

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

    private static final BeatmapGeneralParser generalParser = new BeatmapGeneralParser();
    private static final BeatmapMetadataParser metadataParser = new BeatmapMetadataParser();
    private static final BeatmapDifficultyParser difficultyParser = new BeatmapDifficultyParser();
    private static final BeatmapEventsParser eventsParser = new BeatmapEventsParser();
    private static final BeatmapControlPointsParser controlPointsParser = new BeatmapControlPointsParser();
    private static final BeatmapColorParser colorParser = new BeatmapColorParser();
    private static final BeatmapHitObjectsParser hitObjectsParser = new BeatmapHitObjectsParser();

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
                closeSource();
                return false;
            }

            Pattern pattern = Pattern.compile("osu file format v(\\d+)");
            Matcher matcher = pattern.matcher(head);
            if (!matcher.find()) {
                closeSource();
                return false;
            }

            int formatPos = head.indexOf("file format v");
            beatmapFormatVersion = Utils.tryParseInt(head.substring(formatPos + 13), beatmapFormatVersion);
        } catch (Exception e) {
            Log.e("BeatmapParser.openFile", e.getMessage());
        }

        return true;
    }

    /**
     * Parses the <code>.osu</code> file.
     *
     * @param withHitObjects Whether to parse hit objects. This will improve parsing time significantly.
     * @return A <code>BeatmapData</code> containing relevant information of the beatmap file,
     * <code>null</code> if the beatmap file cannot be opened or a line could not be parsed.
     */
    public BeatmapData parse(boolean withHitObjects) {
        String fileName = file.getName().substring(0, file.getName().length() - 4);

        if (source == null && !openFile()) {
            ToastLogger.showText(
                    StringTable.format(R.string.beatmap_parser_cannot_open_file, fileName),
                    true
            );
            return null;
        }

        BeatmapSection currentSection = null;
        BeatmapData data = new BeatmapData();

        data.setMD5(FileUtils.getMD5Checksum(file));
        data.setFolder(file.getParent());
        data.setFilename(file.getPath());
        data.setFormatVersion(beatmapFormatVersion);

        String s;

        try {
            while ((s = source.readUtf8Line()) != null) {
                // Check if beatmap is not an osu!standard beatmap
                if (data.general.mode != 0) {
                    // Silently ignore (do not log anything to the user)
                    return null;
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
                if (s.startsWith("[") && s.endsWith("]")) {
                    currentSection = BeatmapSection.parse(s.substring(1, s.length() - 1));
                    continue;
                }

                if (currentSection == null) {
                    continue;
                }

                try {
                    switch (currentSection) {
                        case general:
                            generalParser.parse(data, s);
                            break;
                        case metadata:
                            metadataParser.parse(data, s);
                            break;
                        case difficulty:
                            difficultyParser.parse(data, s);
                            break;
                        case events:
                            eventsParser.parse(data, s);
                            break;
                        case timingPoints:
                            controlPointsParser.parse(data, s);
                            break;
                        case colors:
                            colorParser.parse(data, s);
                            break;
                        case hitObjects:
                            if (withHitObjects) {
                                hitObjectsParser.parse(data, s);
                            }
                            break;
                    }
                } catch (Exception e) {
                    Log.e("BeatmapParser.parse", "Unable to parse line " + s, e);
                }
            }

            closeSource();
            populateObjectData(data);
        } catch (IOException e) {
            Log.e("BeatmapParser.parse", e.getMessage());
            return null;
        }

        return data;
    }

    /**
     * Populates the object scales of a <code>BeatmapData</code>.
     *
     * @param data The <code>BeatmapData</code> whose object scales will be populated.
     */
    public static void populateObjectData(final BeatmapData data) {
        float scale = (1 - 0.7f * (data.difficulty.cs - 5) / 5) / 2;

        for (HitObject object : data.hitObjects.getObjects()) {
            object.setScale(scale);

            // Reset stack height as we will be re-applying it.
            object.setStackHeight(0);
        }

        HitObjectStackEvaluator.applyStacking(data.getFormatVersion(), data.hitObjects.getObjects(), data.difficulty.ar, data.general.stackLeniency);
    }

    private void closeSource() {
        if (source != null) {
            try {
                source.close();
            } catch (IOException ignored) {}

            source = null;
        }
    }
}
