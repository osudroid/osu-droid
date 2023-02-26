package com.rian.difficultycalculator.beatmap.timings;

/**
 * Represents a control point.
 */
public abstract class ControlPoint {
    /**
     * The time at which this control point takes effect, in milliseconds.
     */
    public final int time;

    /**
     * @param time The time at which this control point takes effect, in milliseconds.
     */
    public ControlPoint(int time) {
        this.time = time;
    }
}
