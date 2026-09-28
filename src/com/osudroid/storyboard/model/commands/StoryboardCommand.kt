package com.osudroid.storyboard.model.commands

import com.osudroid.storyboard.model.StoryboardEasing

/**
 * Represents a single storyboard command that changes one property of a storyboard element
 * from [startValue] to [endValue] over the interval [startTime]..[endTime].
 */
class StoryboardCommand<T>(
    /**
     * The easing applied to the interpolation of this command.
     */
    @JvmField
    val easing: StoryboardEasing,

    /**
     * The start time of this command in milliseconds.
     */
    @JvmField
    val startTime: Double,

    /**
     * The end time of this command in milliseconds.
     */
    @JvmField
    val endTime: Double,

    /**
     * The value of the property at [startTime].
     */
    @JvmField
    val startValue: T,

    /**
     * The value of the property at [endTime].
     */
    @JvmField
    val endValue: T
) {
    /**
     * The duration of this command in milliseconds.
     */
    val duration
        get() = endTime - startTime

    /**
     * Returns the eased interpolation progress of this command at the given time, clamped to `[0, 1]`.
     *
     * @param time The time in milliseconds.
     */
    fun progressAt(time: Double): Double {
        if (duration <= 0) {
            return 1.0
        }

        val progress = ((time - startTime) / duration).coerceIn(0.0, 1.0)
        return easing.interpolate(progress)
    }

    /**
     * Returns a copy of this command with its times shifted by [offset] milliseconds.
     *
     * @param offset The offset to shift by.
     */
    fun shifted(offset: Double) = StoryboardCommand(easing, startTime + offset, endTime + offset, startValue, endValue)

    override fun toString() = "$startTime -> $endTime, $startValue -> $endValue, $easing"
}
