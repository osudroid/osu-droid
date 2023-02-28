package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;

/**
 * Represents a hit object that ends at a different time than its start time.
 */
public abstract class HitObjectWithDuration extends HitObject {
    /**
     * The time at which this hit object ends, in milliseconds.
     */
    protected double endTime;

    /**
     * @param startTime The time at which this hit object starts, in milliseconds.
     * @param endTime   The time at which this hit object ends, in milliseconds.
     * @param position  The position of the hit object relative to the play field.
     */
    public HitObjectWithDuration(double startTime, double endTime, Vector2 position) {
        super(startTime, position);

        this.endTime = endTime;
    }

    /**
     * @param startTime The time at which this hit object starts, in milliseconds.
     * @param endTime   The time at which this hit object ends, in milliseconds.
     * @param x         The X position of the hit object relative to the play field.
     * @param y         The Y position of the hit object relative to the play field.
     */
    public HitObjectWithDuration(double startTime, double endTime, double x, double y) {
        this(startTime, endTime, new Vector2(x, y));
    }

    /**
     * Gets the end time of this hit object.
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Gets the duration of this hit object.
     */
    public double getDuration() {
        return endTime - startTime;
    }
}
