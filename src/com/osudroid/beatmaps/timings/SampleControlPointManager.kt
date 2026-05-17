package com.osudroid.beatmaps.timings

import com.osudroid.beatmaps.constants.SampleBank

/**
 * A manager for [SampleControlPoint]s.
 */
class SampleControlPointManager : ControlPointManager<SampleControlPoint>(
    SampleControlPoint(0.0, SampleBank.Normal, 100, 0)
) {
    override fun controlPointAt(time: Double) = binarySearchWithFallback(time, controlPoints.getOrNull(0) ?: defaultControlPoint)
}