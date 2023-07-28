package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents a hit object that can be nested into a slider.
 */
public abstract class SliderHitObject extends HitObject {
    /**
     * The index of the span at which this slider hit object lies.
     */
    protected final int spanIndex;

    /**
     * The start time of the span at which this slider hit object lies, in milliseconds.
     */
    protected final double spanStartTime;

    /**
     * @param startTime              The time at which this slider hit object starts, in milliseconds.
     * @param x                      The X position of the slider hit object relative to the play field.
     * @param y                      The Y position of the slider hit object relative to the play field.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderHitObject(double startTime, float x, float y, int spanIndex, double spanStartTime) {
        this(startTime, new Vector2(x, y), spanIndex, spanStartTime);
    }

    /**
     * @param startTime              The time at which this hit object starts, in milliseconds.
     * @param position               The position of the hit object relative to the play field.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderHitObject(double startTime, Vector2 position, int spanIndex, double spanStartTime) {
        super(startTime, position);

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
