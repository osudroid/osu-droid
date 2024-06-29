package com.rian.osu.beatmap.timings

/**
 * Represents a [ControlPoint] that applies an effect to a beatmap.
 */
class EffectControlPoint(
    /**
     * The time at which this [EffectControlPoint] takes effect, in milliseconds.
     */
    time: Double,

    /**
     * Whether kiai time is enabled at this [EffectControlPoint].
     */
    @JvmField
    val isKiai: Boolean
): ControlPoint(time) {
    override fun isRedundant(existing: ControlPoint) =
        existing is EffectControlPoint &&
        isKiai == existing.isKiai
}