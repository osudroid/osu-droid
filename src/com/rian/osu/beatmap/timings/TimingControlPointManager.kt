package com.rian.osu.beatmap.timings

/**
 * A manager for timing control points.
 */
class TimingControlPointManager : ControlPointManager<TimingControlPoint>(
    TimingControlPoint(0.0, 1000.0, 4)
) {
    override fun controlPointAt(time: Double) =
        binarySearchWithFallback(time, controlPoints.getOrNull(0) ?: defaultControlPoint)

    override fun clone() = super.clone() as TimingControlPointManager
}
