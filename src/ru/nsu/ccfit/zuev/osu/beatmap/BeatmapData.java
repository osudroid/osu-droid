package ru.nsu.ccfit.zuev.osu.beatmap;

import com.rian.difficultycalculator.beatmap.BeatmapControlPointsManager;
import com.rian.difficultycalculator.beatmap.BeatmapHitObjectsManager;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapColor;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapDifficulty;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapEvents;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapGeneral;
import ru.nsu.ccfit.zuev.osu.beatmap.sections.BeatmapMetadata;

/**
 * A structure containing information about a beatmap.
 */
public class BeatmapData {
    /**
     * General information about this beatmap.
     */
    public final BeatmapGeneral general;

    /**
     * Information used to identify this beatmap.
     */
    public final BeatmapMetadata metadata;

    /**
     * Difficulty settings of this beatmap.
     */
    public final BeatmapDifficulty difficulty;

    /**
     * Events of this beatmap.
     */
    public final BeatmapEvents events;

    /**
     * Combo and skin colors of this beatmap.
     */
    public final BeatmapColor colors;

    /**
     * Raw timing points data in this beatmap.
     */
    public final ArrayList<String> rawTimingPoints = new ArrayList<>();

    /**
     * The manager of timing points in this beatmap.
     */
    public final BeatmapControlPointsManager timingPoints;

    /**
     * Raw hit objects data in this beatmap.
     */
    public final ArrayList<String> rawHitObjects = new ArrayList<>();

    /**
     * The manager of hit objects in this beatmap.
     */
    public final BeatmapHitObjectsManager hitObjects;

    /**
     * The path of parent folder of this beatmap.
     */
    private String folder;

    /**
     * The format version of this beatmap.
     */
    private int formatVersion = 14;

    public BeatmapData() {
        general = new BeatmapGeneral();
        metadata = new BeatmapMetadata();
        difficulty = new BeatmapDifficulty();
        events = new BeatmapEvents();
        colors = new BeatmapColor();
        timingPoints = new BeatmapControlPointsManager();
        hitObjects = new BeatmapHitObjectsManager();
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapData(BeatmapData source) {
        folder = source.folder;
        formatVersion = source.formatVersion;

        general = source.general.deepClone();
        metadata = source.metadata.deepClone();
        difficulty = source.difficulty.deepClone();
        events = source.events.deepClone();
        colors = source.colors.deepClone();
        timingPoints = source.timingPoints.deepClone();
        hitObjects = source.hitObjects.deepClone();

        rawTimingPoints.addAll(source.rawTimingPoints);
        rawHitObjects.addAll(source.rawHitObjects);
    }

    /**
     * Deep clones this instance.
     *
     * @return The deep cloned instance.
     */
    public BeatmapData deepClone() {
        return new BeatmapData(this);
    }

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
