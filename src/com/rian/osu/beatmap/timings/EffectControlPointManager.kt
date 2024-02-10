package com.rian.osu.beatmap.timings

/**
 * A manager for effect control points.
 */
class EffectControlPointManager : ControlPointManager<EffectControlPoint>(
    EffectControlPoint(0.0, false)
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time)

    override fun clone() = super.clone() as EffectControlPointManager
}