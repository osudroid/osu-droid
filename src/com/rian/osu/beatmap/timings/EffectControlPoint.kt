package com.rian.osu.beatmap.timings

/**
 * Represents a control point that applies an effect to a beatmap.
 */
class EffectControlPoint(
    /**
     * The time at which this control point takes effect, in milliseconds.
     */
    time: Double,

    /**
     * Whether kiai time is enabled at this control point.
     */
    @JvmField
    val isKiai: Boolean
): ControlPoint(time) {
    override fun isRedundant(existing: ControlPoint) =
        existing is EffectControlPoint &&
        isKiai == existing.isKiai

    override fun clone() = super.clone() as EffectControlPoint
}