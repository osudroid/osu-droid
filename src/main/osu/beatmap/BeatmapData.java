package main.osu.beatmap;

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.BeatmapHitObjectsManager;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;

import java.util.ArrayList;

import main.osu.beatmap.sections.BeatmapColor;
import main.osu.beatmap.sections.BeatmapDifficulty;
import main.osu.beatmap.sections.BeatmapEvents;
import main.osu.beatmap.sections.BeatmapGeneral;
import main.osu.beatmap.sections.BeatmapMetadata;

/**
 * A structure containing information about a beatmap.
 */
public class BeatmapData {
    /**
     * General information about this beatmap.
     */
    public final BeatmapGeneral general = new BeatmapGeneral();

    /**
     * Information used to identify this beatmap.
     */
    public final BeatmapMetadata metadata = new BeatmapMetadata();

    /**
     * Difficulty settings of this beatmap.
     */
    public final BeatmapDifficulty difficulty = new BeatmapDifficulty();

    /**
     * Events of this beatmap.
     */
    public final BeatmapEvents events = new BeatmapEvents();

    /**
     * Combo and skin colors of this beatmap.
     */
    public final BeatmapColor colors = new BeatmapColor();

    /**
     * The manager of timing points in this beatmap.
     */
    public final BeatmapControlPointsManager timingPoints = new BeatmapControlPointsManager();

    /**
     * Raw data of hit objects in this beatmap.
     */
    public final ArrayList<String> rawHitObjects = new ArrayList<>();

    /**
     * The manager of hit objects in this beatmap.
     */
    public final BeatmapHitObjectsManager hitObjects = new BeatmapHitObjectsManager();

    /**
     * The path of parent folder of this beatmap.
     */
    private String folder;

    /**
     * The format version of this beatmap.
     */
    private int formatVersion = 14;

    /**
     * Gets the path of the parent folder of this beatmap.
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the path of the parent folder of this beatmap.
     *
     * @param path The path of the parent folder.
     */
    public void setFolder(String path) {
        folder = path;
    }

    /**
     * Gets the max combo of this beatmap.
     */
    public int getMaxCombo() {
        int combo = 0;

        for (HitObject object : hitObjects.getObjects()) {
            ++combo;

            if (object instanceof Slider) {
                combo += ((Slider) object).getNestedHitObjects().size() - 1;
            }
        }

        return combo;
    }

    /**
     * Gets the format version of this beatmap.
     */
    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * Sets the format version of this beatmap.
     *
     * @param formatVersion The format version of this beatmap.
     */
    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    public double getOffsetTime(double time) {
        return time + (formatVersion < 5 ? 24 : 0);
    }

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    public int getOffsetTime(int time) {
        return time + (formatVersion < 5 ? 24 : 0);
    }
}
