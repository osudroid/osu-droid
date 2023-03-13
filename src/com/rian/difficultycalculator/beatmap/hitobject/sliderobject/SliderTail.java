package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents a slider tail.
 */
public class SliderTail extends SliderHitObject {
    /**
     * @param startTime              The time at which this slider tail starts, in milliseconds.
     * @param x                      The X position of the slider tail relative to the play field.
     * @param y                      The Y position of the slider tail relative to the play field.
     * @param spanIndex              The index of the span at which this slider tail lies.
     * @param spanStartTime          The start time of the span at which this slider tail lies, in milliseconds.
     */
    public SliderTail(double startTime, double x, double y, int spanIndex, double spanStartTime) {
        super(startTime, new Vector2(x, y), spanIndex, spanStartTime);
    }

    /**
     * @param startTime              The time at which this slider tail starts, in milliseconds.
     * @param position               The position of the slider tail relative to the play field.
     * @param spanIndex              The index of the span at which this slider tail lies.
     * @param spanStartTime          The start time of the span at which this slider tail lies, in milliseconds.
     */
    public SliderTail(double startTime, Vector2 position, int spanIndex, double spanStartTime) {
        super(startTime, position, spanIndex, spanStartTime);
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private SliderTail(SliderTail source) {
        this(source.startTime, source.position.x, source.position.y, source.spanIndex, source.spanStartTime);
    }

    @Override
    public SliderTail deepClone() {
        return new SliderTail(this);
    }
}
