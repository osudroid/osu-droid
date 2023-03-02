package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.checkers.SliderCheeseInformation;
import com.rian.difficultycalculator.math.MathUtils;

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
    public double tapPenalty = 1;

    /**
     * The aim slider cheese penalty to apply for penalized scores. This value can be properly
     * obtained by passing the replay relating to this score to the slider cheese checker.
     * <br><br>
     * This is only used in rimu! and must be between 0 (exclusive) and 1 (inclusive).
     */
    public double aimSliderCheesePenalty = 1;

    /**
     * The flashlight slider cheese penalty to apply for penalized scores. This value can be properly
     * obtained by passing the replay relating to this score to the slider cheese checker.
     * <br><br>
     * This is only used in rimu! and must be between 0 (exclusive) and 1 (inclusive).
     */
    public double flashlightSliderCheesePenalty = 1;

    /**
     * The visual slider cheese penalty to apply for penalized scores. This value can be properly
     * obtained by passing the replay relating to this score to the slider cheese checker.
     * <br><br>
     * This is only used in rimu! and must be between 0 (exclusive) and 1 (inclusive).
     */
    public double visualSliderCheesePenalty = 1;

    /**
     * Applies a slider cheese information to this parameter.
     *
     * @param sliderCheeseInformation The slider cheese information to apply.
     */
    public void applySliderCheeseInformation(SliderCheeseInformation sliderCheeseInformation) {
        aimSliderCheesePenalty = sliderCheeseInformation.aimPenalty;
        flashlightSliderCheesePenalty = sliderCheeseInformation.flashlightPenalty;
        visualSliderCheesePenalty = sliderCheeseInformation.visualPenalty;
    }
}
