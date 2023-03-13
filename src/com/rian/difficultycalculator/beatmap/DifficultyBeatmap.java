package com.rian.difficultycalculator.beatmap;

import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;

/**
 * A beatmap structure containing necessary information for difficulty and performance calculation.
 */
public class DifficultyBeatmap {
    /**
     * The format version of this beatmap.
     */
    private int formatVersion = 14;

    /**
     * The manager for difficulty settings of this beatmap.
     */
    private BeatmapDifficultyManager difficultyManager = new BeatmapDifficultyManager();

    /**
     * The manager for hit objects of this beatmap.
     */
    private BeatmapHitObjectsManager hitObjectsManager = new BeatmapHitObjectsManager();

    public DifficultyBeatmap() {}

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private DifficultyBeatmap(DifficultyBeatmap source) {
        formatVersion = source.formatVersion;
        difficultyManager = source.difficultyManager.deepClone();
        hitObjectsManager = source.hitObjectsManager.deepClone();
    }

    /**
     * Gets the difficulty manager of this beatmap.
     */
    public BeatmapDifficultyManager getDifficultyManager() {
        return difficultyManager;
    }

    /**
     * Gets the hit object manager of this beatmap.
     */
    public BeatmapHitObjectsManager getHitObjectsManager() {
        return hitObjectsManager;
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
     * @param formatVersion The new format version of this beatmap.
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
        return time + (this.formatVersion < 5 ? 24 : 0);
    }

    /**
     * Gets the max combo of this beatmap.
     */
    public int getMaxCombo() {
        int combo = 0;

        for (HitObject object : hitObjectsManager.getObjects()) {
            ++combo;

            if (object instanceof Slider) {
                combo += ((Slider) object).getNestedHitObjects().size() - 1;
            }
        }

        return combo;
    }

    /**
     * Deep clones this beatmap.
     *
     * @return The deep cloned instance of this beatmap.
     */
    public DifficultyBeatmap deepClone() {
        return new DifficultyBeatmap(this);
    }
}
