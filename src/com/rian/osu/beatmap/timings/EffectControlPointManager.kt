package com.rian.osu.beatmap.timings

/**
 * A manager for [EffectControlPoint]s.
 */
class EffectControlPointManager : ControlPointManager<EffectControlPoint>(
    EffectControlPoint(0.0, false)
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time)
}