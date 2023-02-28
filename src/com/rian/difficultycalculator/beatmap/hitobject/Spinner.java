package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents a spinner.
 */
public class Spinner extends HitObjectWithDuration {
    /**
     * @param startTime The time at which this spinner starts, in milliseconds.
     * @param endTime   The time at which this spinner ends, in milliseconds.
     */
    public Spinner(double startTime, double endTime) {
        super(startTime, endTime, new Vector2(256, 192));
    }

    @Override
    public Spinner deepClone() {
        return new Spinner(startTime, endTime);
    }
}
