package com.rian.osu.beatmap.timings

/**
 * A manager for difficulty control points.
 */
class DifficultyControlPointManager : ControlPointManager<DifficultyControlPoint>(
    DifficultyControlPoint(0.0, 1.0, true)
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time)

    override fun clone() = super.clone() as DifficultyControlPointManager
}
