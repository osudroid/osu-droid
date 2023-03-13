package com.rian.difficultycalculator.beatmap.timings;

/**
 * Represents a break period.
 */
public final class BreakPeriod {
    /**
     * The time at which this break period starts, in milliseconds.
     */
    public final double startTime;

    /**
     * The time at which this break period ends, in milliseconds.
     */
    public final double endTime;

    /**
     * @param startTime The time at which this break period starts, in milliseconds.
     * @param endTime The time at which this break period ends, in milliseconds.
     */
    public BreakPeriod(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets the duration of this break period.
     */
    public double getDuration() {
        return endTime - startTime;
    }
}
