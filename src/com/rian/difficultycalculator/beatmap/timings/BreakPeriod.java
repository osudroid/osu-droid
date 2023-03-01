package com.rian.difficultycalculator.beatmap.timings;

/**
 * Represents a break period.
 */
public final class BreakPeriod {
    /**
     * The time at which this break period starts, in milliseconds.
     */
    public final int startTime;

    /**
     * The time at which this break period ends, in milliseconds.
     */
    public final int endTime;

    /**
     * @param startTime The time at which this break period starts, in milliseconds.
     * @param endTime The time at which this break period ends, in milliseconds.
     */
    public BreakPeriod(int startTime, int endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Gets the duration of this break period.
     */
    public int getDuration() {
        return endTime - startTime;
    }
}
