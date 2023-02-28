package com.rian.difficultycalculator.beatmap.hitobject.sliderobject;

import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents the head of a slider.
 */
public class SliderHead extends SliderHitObject {
    /**
     * @param startTime              The start time of this slider head, in milliseconds.
     * @param x                      The X position of this slider head relative to the play field.
     * @param y                      The Y position of this slider head relative to the play field.
     */
    public SliderHead(double startTime, double x, double y) {
        this(startTime, new Vector2(x, y));
    }

    /**
     * @param startTime              The start time of this slider head, in milliseconds.
     * @param position               The position of this slider head relative to the play field.
     */
    public SliderHead(double startTime, Vector2 position) {
        super(startTime, position, 0, startTime);
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private SliderHead(SliderHead source) {
        this(source.startTime, source.position.x, source.position.y);
    }

    @Override
    public SliderHead deepClone() {
        return new SliderHead(this);
    }
}
