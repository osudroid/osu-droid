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
     * The multiplier for the threshold in time where hit objects placed close together stack, ranging from 0 to 1.
     */
    private float stackLeniency = 0.7f;

    /**
     * The manager for difficulty settings of this beatmap.
     */
    private final BeatmapDifficultyManager difficultyManager;

    /**
     * The manager for hit objects of this beatmap.
     */
    private final BeatmapHitObjectsManager hitObjectsManager;

    /**
     * Constructs a new instance of <code>DifficultyBeatmap</code> using an existing
     * difficulty manager.
     * <br><br>
     * The manager will be deep-cloned.
     *
     * @param difficultyManager The difficulty manager.
     */
    public DifficultyBeatmap(BeatmapDifficultyManager difficultyManager) {
        this.difficultyManager = difficultyManager.deepClone();
        this.hitObjectsManager = new BeatmapHitObjectsManager();
    }

    /**
     * Constructs a new instance of <code>DifficultyBeatmap</code> using an existing
     * difficulty manager and hit objects manager.
     * <br><br>
     * Both managers will be deep-cloned.
     *
     * @param difficultyManager The difficulty manager.
     * @param hitObjectsManager The hit objects manager.
     */
    public DifficultyBeatmap(BeatmapDifficultyManager difficultyManager, BeatmapHitObjectsManager hitObjectsManager) {
        this.difficultyManager = difficultyManager.deepClone();
        this.hitObjectsManager = hitObjectsManager.deepClone();
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private DifficultyBeatmap(DifficultyBeatmap source) {
        this(source.difficultyManager, source.hitObjectsManager);

        formatVersion = source.formatVersion;
        stackLeniency = source.stackLeniency;
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
     * Gets the multiplier for the threshold in time where hit objects placed close together stack, ranging from 0 to 1.
     */
    public float getStackLeniency() {
        return stackLeniency;
    }

    /**
     * Sets the multiplier for the threshold in time where hit objects placed close together stack.
     *
     * @param stackLeniency The new multiplier, ranging from 0 to 1.
     */
    public void setStackLeniency(float stackLeniency) {
        this.stackLeniency = stackLeniency;
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
