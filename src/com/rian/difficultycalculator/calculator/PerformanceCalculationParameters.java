package com.rian.difficultycalculator.calculator;

/**
 * A class for specifying parameters for performance calculation.
 */
public class PerformanceCalculationParameters {
    /**
     * The maximum combo achieved.
     */
    public int maxCombo;

    /**
     * The amount of 300 (great) hits achieved.
     */
    public int countGreat;

    /**
     * The amount of 100 (ok) hits achieved.
     */
    public int countOk;

    /**
     * The amount of 50 (meh) hits achieved.
     */
    public int countMeh;

    /**
     * The amount of misses achieved.
     */
    public int countMiss;

    /**
     * The tap penalty to apply. This value can be properly obtained by passing the replay
     * relating to this score to the three finger checker.
     * <br><br>
     * This is only used in rimu! and must be greater than or equal to 1.
     */
    public int tapPenalty = 1;
}
