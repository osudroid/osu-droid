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
    var aim: Double = 1.0,
) {
    /**
     * Copies this [SliderCheesePenalty] to another [SliderCheesePenalty].
     *
     * @param other The other [SliderCheesePenalty].
     */
    fun copyTo(other: SliderCheesePenalty) {
        other.aim = aim
    }

    /**
     * Resets this [SliderCheesePenalty] to its initial state.
     */
    fun reset() {
        aim = 1.0
    }
}
