package ru.nsu.ccfit.zuev.osu.beatmap.parser;

import android.util.Log;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.HitObjectWithDuration;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
import com.rian.difficultycalculator.utils.HitObjectStackEvaluator;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.constants.BeatmapSection;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator;
import ru.nsu.ccfit.zuev.osuplus.R;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapColorParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapControlPointsParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapDifficultyParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapEventsParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapGeneralParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapHitObjectsParser;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.sections.BeatmapMetadataParser;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
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
        data.setFormatVersion(beatmapFormatVersion);

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
                    closeSource();
                    return null;
                }
            }

            closeSource();
            populateObjectData(data);
        } catch (IOException e) {
            Log.e("BeatmapParser.parse", e.getMessage());
            return data;
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
        }

        HitObjectStackEvaluator.applyStandardStacking(data.getFormatVersion(), data.hitObjects.getObjects(), data.difficulty.ar, data.general.stackLeniency);
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
