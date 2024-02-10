package com.rian.osu.beatmap.timings

/**
 * Represents a break period.
 */
data class BreakPeriod(
    /**
     * The time at which the break period starts, in milliseconds.
     */
    @JvmField
    val startTime: Float,

    /**
     * The time at which the break period ends, in milliseconds.
     */
    @JvmField
    val endTime: Float
) {
    /**
     * The duration of this break period, in milliseconds
     */
    val duration: Float
        get() = endTime - startTime
}
