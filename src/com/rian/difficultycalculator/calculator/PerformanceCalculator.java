package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.attributes.PerformanceAttributes;

/**
 * A performance calculator for calculating performance points.
 */
public abstract class PerformanceCalculator {
    /**
     * The difficulty attributes being calculated.
     */
    public final DifficultyAttributes difficultyAttributes;

    protected int scoreMaxCombo;
    protected int countGreat;
    protected int countOk;
    protected int countMeh;
    protected int countMiss;

    public PerformanceCalculator(DifficultyAttributes attributes) {
        this.difficultyAttributes = attributes;

        processParameters(null);
    }

    /**
     * Calculates the performance value of the difficulty attributes assuming an SS score.
     *
     * @return The performance attributes for the beatmap assuming an SS score.
     */
    public PerformanceAttributes calculate() {
        return createPerformanceAttributes(null);
    }

    /**
     * Calculates the performance value of the difficulty attributes with the specified parameters.
     *
     * @param parameters The parameters to create the attributes for.
     * @return The performance attributes for the beatmap relating to the parameters.
     */
    public PerformanceAttributes calculate(PerformanceCalculationParameters parameters) {
        processParameters(parameters);

        return createPerformanceAttributes(parameters);
    }

    /**
     * Creates the performance attributes of the difficulty attributes.
     *
     * @param parameters The parameters to create the attributes for.
     * @return The performance attributes for the beatmap relating to the parameters.
     */
    protected abstract PerformanceAttributes createPerformanceAttributes(PerformanceCalculationParameters parameters);

    protected void processParameters(PerformanceCalculationParameters parameters) {
        if (parameters == null) {
            resetDefaults();
            return;
        }

        scoreMaxCombo = parameters.maxCombo;
        countGreat = parameters.countGreat;
        countOk = parameters.countOk;
        countMeh = parameters.countMeh;
        countMiss = parameters.countMiss;
    }

    /**
     * Calculates the accuracy of the parameters.
     */
    protected double getAccuracy() {
        return (double) (countGreat * 6 + countOk * 2 + countMeh) / (getTotalHits() * 6);
    }

    /**
     * Gets the total hits that can be done in the beatmap.
     */
    protected int getTotalHits() {
        return difficultyAttributes.hitCircleCount + difficultyAttributes.sliderCount + difficultyAttributes.spinnerCount;
    }

    /**
     * Gets the amount of hits that were successfully done.
     */
    protected int getTotalSuccessfulHits() {
        return countGreat + countOk + countMeh;
    }

    /**
     * Resets this calculator to its original state.
     */
    protected void resetDefaults() {
        scoreMaxCombo = difficultyAttributes.maxCombo;
        countGreat = getTotalHits();
        countOk = 0;
        countMeh = 0;
        countMiss = 0;
    }
}
