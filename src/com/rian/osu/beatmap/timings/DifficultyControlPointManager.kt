package com.rian.osu.beatmap.timings

/**
 * A manager for [DifficultyControlPoint]s.
 */
class DifficultyControlPointManager : ControlPointManager<DifficultyControlPoint>(
    DifficultyControlPoint(0.0, 1.0, true)
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time)
}
