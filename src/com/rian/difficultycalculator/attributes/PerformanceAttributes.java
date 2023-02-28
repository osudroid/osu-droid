package com.rian.difficultycalculator.attributes;

/**
 * A structure containing the performance values of a score.
 */
public abstract class PerformanceAttributes {
    /**
     * Calculated score performance points.
     */
    public double total;

    /**
     * The aim performance value.
     */
    public double aim;

    /**
     * The accuracy performance value.
     */
    public double accuracy;

    /**
     * The flashlight performance value.
     */
    public double flashlight;

    /**
     * The amount of misses that are filtered out from slider breaks.
     */
    public double effectiveMissCount;
}
