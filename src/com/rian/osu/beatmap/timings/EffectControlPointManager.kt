package com.rian.osu.beatmap.timings

/**
 * A manager for [EffectControlPoint]s.
 */
class EffectControlPointManager : ControlPointManager<EffectControlPoint>(
    DEFAULT_EFFECT_CONTROL_POINT
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time)

    companion object {
        val DEFAULT_EFFECT_CONTROL_POINT = EffectControlPoint(0.0, false)
    }
}