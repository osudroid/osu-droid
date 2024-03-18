package com.rian.osu.difficulty.calculator

import com.rian.osu.replay.SliderCheesePenalty

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
     * The slider cheese penalties to apply for penalized scores.
     */
    @JvmField
    var sliderCheesePenalty = SliderCheesePenalty()
}