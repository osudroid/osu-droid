package com.rian.difficultycalculator.hitobject.sliderobject;

import com.rian.difficultycalculator.hitobject.HitObject;
import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.timings.TimingControlPoint;

/**
 * Represents a hit object that can be nested into a slider.
 */
public abstract class SliderHitObject extends HitObject {
    /**
     * The index of the span at which this slider hit object lies.
     */
    private final int spanIndex;

    /**
     * The start time of the span at which this slider hit object lies, in milliseconds.
     */
    private final double spanStartTime;

    /**
     * @param startTime              The time at which this slider hit object starts, in milliseconds.
     * @param x                      The X position of the slider hit object relative to the play field.
     * @param y                      The Y position of the slider hit object relative to the play field.
     * @param timingControlPoint     The timing control point this slider hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider hit object is under effect on.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderHitObject(double startTime, double x, double y,
                           TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                           int spanIndex, double spanStartTime) {
        this(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint, spanIndex, spanStartTime);
    }

    /**
     * @param startTime              The time at which this hit object starts, in milliseconds.
     * @param position               The position of the hit object relative to the play field.
     * @param timingControlPoint     The timing control point this hit object is under effect on.
     * @param difficultyControlPoint The difficulty control point this hit object is under effect on.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderHitObject(double startTime, Vector2 position,
                           TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                           int spanIndex, double spanStartTime) {
        super(startTime, position, timingControlPoint, difficultyControlPoint);

        this.spanIndex = spanIndex;
        this.spanStartTime = spanStartTime;
    }

    /**
     * Gets the index of the span at which this slider hit object lies.
     */
    public int getSpanIndex() {
        return spanIndex;
    }

    /**
     * Gets the start time of the span at which this slider hit object lies, in milliseconds.
     */
    public double getSpanStartTime() {
        return spanStartTime;
    }
}
