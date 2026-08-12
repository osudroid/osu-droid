package com.osudroid.difficulty.attributes

import com.osudroid.difficulty.calculator.DifficultyCalculator

/**
 * Wraps a [DifficultyAttributes] object and adds a time value for which the attribute is valid.
 *
 * Output by [DifficultyCalculator.calculateTimed] methods.
 */
class TimedDifficultyAttributes<TAttributes : DifficultyAttributes>(
    /**
     * The non-clock-adjusted time value at which the attributes take effect.
     */
    @JvmField
    val time: Double,

    /**
     * The attributes.
     */
    @JvmField
    val attributes: TAttributes,

    /**
     * The number of sliders in the beatmap up to this point.
     */
    @JvmField
    val sliderCount: Int = 0,

    /**
     * The number of slider ticks in the beatmap up to this point.
     */
    @JvmField
    val sliderTickCount: Int = 0,

    /**
     * The number of slider repeats in the beatmap up to this point.
     */
    @JvmField
    val sliderRepeatCount: Int = 0
) : Comparable<TimedDifficultyAttributes<TAttributes>> {
    override fun compareTo(other: TimedDifficultyAttributes<TAttributes>) = time.compareTo(other.time)
}
