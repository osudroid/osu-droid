package com.rian.osu.beatmap.timings

/**
 * Represents a control point.
 */
abstract class ControlPoint(
    /**
     * The time at which this [ControlPoint] takes effect, in milliseconds.
     */
    @JvmField
    val time: Double
) {
    /**
     * Determines whether this [ControlPoint] results in a meaningful change when placed alongside another.
     *
     * @param existing An existing [ControlPoint] to compare with.
     */
    abstract fun isRedundant(existing: ControlPoint): Boolean
}
