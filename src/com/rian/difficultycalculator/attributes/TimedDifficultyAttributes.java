package com.rian.difficultycalculator.attributes;

/**
 * Wraps a <code>DifficultyAttributes</code> object and adds a time value for which the attribute is valid.
 * <br><br>
 * Output by <code>DifficultyCalculator.calculateTimed</code> methods.
 */
public class TimedDifficultyAttributes {
    /**
     * The non-clock-adjusted time value at which the attributes take effect.
     */
    public final double time;

    /**
     * The attributes.
     */
    public final DifficultyAttributes attributes;

    public TimedDifficultyAttributes(double time, DifficultyAttributes attributes) {
        this.time = time;
        this.attributes = attributes;
    }

    public int compareTo(TimedDifficultyAttributes other) {
        return Double.compare(time, other.time);
    }
}
