package com.rian.osu.difficulty.attributes

import com.rian.osu.difficulty.calculator.DifficultyCalculator

/**
 * Wraps a [DifficultyAttributes] object and adds a time value for which the attribute is valid.
 *
 * Output by [DifficultyCalculator.calculateTimed] methods.
 */
class TimedDifficultyAttributes(
    /**
     * The non-clock-adjusted time value at which the attributes take effect.
     */
    @JvmField
    val time: Double,

    /**
     * The attributes.
     */
    @JvmField
    val attributes: DifficultyAttributes
) {
    operator fun compareTo(other: TimedDifficultyAttributes) = time.compareTo(other.time)
}
