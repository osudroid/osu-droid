package com.rian.osu.replay

import com.rian.osu.difficulty.calculator.DroidPerformanceCalculationParameters
import com.rian.osu.difficulty.calculator.DroidPerformanceCalculator

/**
 * Represents slider cheesing penalties that can be passed to a [DroidPerformanceCalculator] via a
 * [DroidPerformanceCalculationParameters].
 */
data class SliderCheesePenalty(
    /**
     * The penalty for aim pp.
     */
    @JvmField
    val aim: Double = 1.0,

    /**
     * The penalty for flashlight pp.
     */
    @JvmField
    val flashlight: Double = 1.0,

    /**
     * The penalty for visual pp.
     */
    @JvmField
    val visual: Double = 1.0
)
