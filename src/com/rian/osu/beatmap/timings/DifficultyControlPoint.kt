package com.rian.osu.beatmap.timings

import com.rian.osu.beatmap.hitobject.sliderobject.SliderTick

/**
 * Represents a [ControlPoint] that changes speed multiplier.
 */
class DifficultyControlPoint(
    /**
     * The time at which this [DifficultyControlPoint] takes effect, in milliseconds.
     */
    time: Double,

    /**
     * The slider speed multiplier of this [DifficultyControlPoint].
     */
    @JvmField
    val speedMultiplier: Double,

    /**
     * Whether [SliderTick]s should be generated at this [DifficultyControlPoint].
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
}
