package com.rian.difficultycalculator.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.timings.TimingControlPoint;

/**
 * Represents a hit object that ends at a different time than its start time.
 */
public abstract class HitObjectWithDuration extends HitObject {
    /**
     * The time at which this hit object ends, in milliseconds.
     */
    protected double endTime;

    /**
     * @param startTime              The time at which this hit object starts, in milliseconds.
     * @param endTime                The time at which this hit object ends, in milliseconds.
     * @param position               The position of the hit object relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     */
    public HitObjectWithDuration(double startTime, double endTime, Vector2 position,
                                 TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        super(startTime, position, timingControlPoint, difficultyControlPoint);

        this.endTime = endTime;
    }

    /**
     * @param startTime              The time at which this hit object starts, in milliseconds.
     * @param endTime                The time at which this hit object ends, in milliseconds.
     * @param x                      The X position of the hit object relative to the play field.
     * @param y                      The Y position of the hit object relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     */
    public HitObjectWithDuration(double startTime, int endTime, double x, double y,
                                 TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint) {
        this(startTime, endTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint);
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
