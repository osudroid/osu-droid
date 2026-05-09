package com.osudroid.replay

import com.osudroid.difficulty.calculator.DroidPerformanceCalculationParameters
import com.osudroid.difficulty.calculator.DroidPerformanceCalculator

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

    /**
     * The penalty for flashlight pp.
     */
    @JvmField
    var flashlight: Double = 1.0,
) {
    /**
     * Copies this [SliderCheesePenalty] to another [SliderCheesePenalty].
     *
     * @param other The other [SliderCheesePenalty].
     */
    fun copyTo(other: SliderCheesePenalty) {
        other.aim = aim
        other.flashlight = flashlight
    }

    /**
     * Resets this [SliderCheesePenalty] to its initial state.
     */
    fun reset() {
        aim = 1.0
        flashlight = 1.0
    }
}
