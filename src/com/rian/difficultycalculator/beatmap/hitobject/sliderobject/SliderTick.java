package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents a slider tick.
 */
public class SliderTick extends SliderHitObject {
    /**
     * @param startTime              The time at which this slider tick starts, in milliseconds.
     * @param x                      The X position of the slider tick relative to the play field.
     * @param y                      The Y position of the slider tick relative to the play field.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderTick(double startTime, double x, double y, int spanIndex, double spanStartTime) {
        this(startTime, new Vector2(x, y), spanIndex, spanStartTime);
    }

    /**
     * @param startTime              The time at which this slider tick starts, in milliseconds.
     * @param position               The position of the slider tick relative to the play field.
     * @param spanIndex              The index of the span at which this slider hit object lies.
     * @param spanStartTime          The start time of the span at which this slider hit object lies, in milliseconds.
     */
    public SliderTick(double startTime, Vector2 position, int spanIndex, double spanStartTime) {
        super(startTime, position, spanIndex, spanStartTime);
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private SliderTick(SliderTick source) {
        this(source.startTime, source.position.x, source.position.y, source.spanIndex, source.spanStartTime);
    }

    @Override
    public SliderTick deepClone() {
        return new SliderTick(this);
    }
}
