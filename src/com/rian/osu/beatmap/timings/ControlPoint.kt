package com.rian.osu.beatmap.timings

/**
 * Represents a control point.
 */
abstract class ControlPoint(
    /**
     * The time at which this control point takes effect, in milliseconds.
     */
    @JvmField
    val time: Double
): Cloneable {
    /**
     * Determines whether this control point results in a meaningful change when placed alongside another.
     *
     * @param existing An existing control point to compare with.
     */
    abstract fun isRedundant(existing: ControlPoint): Boolean

    public override fun clone() = super.clone() as ControlPoint
}
