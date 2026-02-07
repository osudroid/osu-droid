package com.rian.osu.beatmap.timings

/**
 * A manager for [TimingControlPoint]s.
 */
class TimingControlPointManager : ControlPointManager<TimingControlPoint>(
    DEFAULT_TIMING_CONTROL_POINT
) {
    override fun controlPointAt(time: Double) =
        binarySearchWithFallback(time, controlPoints.getOrNull(0) ?: defaultControlPoint)

    companion object {
        val DEFAULT_TIMING_CONTROL_POINT = TimingControlPoint(0.0, 1000.0, 4)
    }
}
