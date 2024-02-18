package com.rian.osu.difficulty.calculator

/**
 * A class for specifying parameters for osu!droid performance calculation.
 */
class DroidPerformanceCalculationParameters : PerformanceCalculationParameters() {
    /**
     * The tap penalty to apply for penalized scores.
     */
    @JvmField
    var tapPenalty = 1.0

    /**
     * The aim slider cheese penalty to apply for penalized scores.
     */
    @JvmField
    var aimSliderCheesePenalty = 1.0

    /**
     * The flashlight slider cheese penalty to apply for penalized scores.
     */
    @JvmField
    var flashlightSliderCheesePenalty = 1.0

    /**
     * The visual slider cheese penalty to apply for penalized scores.
     */
    @JvmField
    var visualSliderCheesePenalty = 1.0
}