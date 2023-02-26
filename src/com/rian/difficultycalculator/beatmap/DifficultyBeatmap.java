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
     * The hit objects of this beatmap.
     */
    public final BeatmapHitObjects hitObjects = new BeatmapHitObjects();

    /**
     * The control points of this beatmap.
     */
    public final BeatmapControlPoints controlPoints = new BeatmapControlPoints();

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

        for (HitObject object : hitObjects.getObjects()) {
            ++combo;

            if (object instanceof Slider) {
                combo += ((Slider) object).getNestedHitObjects().size() - 1;
            }
        }

        return combo;
    }
}
