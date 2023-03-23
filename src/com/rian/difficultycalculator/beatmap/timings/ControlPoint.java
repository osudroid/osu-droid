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

    /**
     * Determines whether this control point results in a meaningful change when placed alongside another.
     *
     * @param existing An existing control point to compare with.
     */
    public abstract boolean isRedundant(ControlPoint existing);

    /**
     * Deep clones this control point.
     *
     * @return The deep cloned control point.
     */
    public ControlPoint deepClone() {
        return null;
    }
}
