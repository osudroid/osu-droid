package com.rian.osu.beatmap.timings

/**
 * Represents a control point that changes speed multiplier.
 */
class DifficultyControlPoint(
    /**
     * The time at which this control point takes effect, in milliseconds.
     */
    time: Double,

    /**
     * The slider speed multiplier of this control point.
     */
    @JvmField
    val speedMultiplier: Double,

    /**
     * Whether slider ticks should be generated at this control point.
     *
     * This exists for backwards compatibility with maps that abuse NaN slider velocity behavior on osu!stable (e.g. /b/2628991).
     */
    @JvmField
    val generateTicks: Boolean
) : ControlPoint(time) {
    override fun isRedundant(existing: ControlPoint) =
        existing is DifficultyControlPoint &&
        speedMultiplier == existing.speedMultiplier &&
        generateTicks == existing.generateTicks

    override fun clone() = super.clone() as DifficultyControlPoint
}
