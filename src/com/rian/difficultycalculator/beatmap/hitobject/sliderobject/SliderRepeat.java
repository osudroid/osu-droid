package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents a slider repeat.
 */
public class SliderRepeat extends SliderHitObject {
    /**
     * @param startTime              The start time of this slider head, in milliseconds.
     * @param x                      The X position of this slider head relative to the play field.
     * @param y                      The Y position of this slider head relative to the play field.
     */
    public SliderRepeat(double startTime, float x, float y, int spanIndex, double spanStartTime) {
        this(startTime, new Vector2(x, y), spanIndex, spanStartTime);
    }

    /**
     * @param startTime              The time at which this slider repeat starts, in milliseconds.
     * @param position               The position of the slider repeat relative to the play field.
     * @param spanIndex              The index of the span at which this slider repeat lies.
     * @param spanStartTime          The start time of the span at which this slider repeat lies, in milliseconds.
     */
    public SliderRepeat(double startTime, Vector2 position, int spanIndex, double spanStartTime) {
        super(startTime, position, spanIndex, spanStartTime);
    }


    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private SliderRepeat(SliderRepeat source) {
        this(source.startTime, source.position.x, source.position.y, source.spanIndex, source.spanStartTime);
    }

    @Override
    public SliderRepeat deepClone() {
        return new SliderRepeat(this);
    }
}
