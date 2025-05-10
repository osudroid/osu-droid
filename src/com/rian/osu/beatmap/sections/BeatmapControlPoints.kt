package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.timings.DifficultyControlPointManager
import com.rian.osu.beatmap.timings.EffectControlPointManager
import com.rian.osu.beatmap.timings.SampleControlPointManager
import com.rian.osu.beatmap.timings.TimingControlPoint
import com.rian.osu.beatmap.timings.TimingControlPointManager
import com.rian.osu.math.Precision
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Contains information about the timing (control) points of a beatmap.
 */
class BeatmapControlPoints {
    /**
     * The manager for timing control points of this beatmap.
     */
    @JvmField
    val timing = TimingControlPointManager()

    /**
     * The manager for difficulty control points of this beatmap.
     */
    @JvmField
    val difficulty = DifficultyControlPointManager()

    /**
     * The manager for effect control points of this beatmap.
     */
    @JvmField
    val effect = EffectControlPointManager()

    /**
     * The manager for sample control points of this beatmap.
     */
    @JvmField
    val sample = SampleControlPointManager()

    /**
     * Obtains the beat divisor closest to [time]. If two are equally close, the smallest divisor is returned.
     *
     * @param time The time to find the closest beat snap divisor for.
     * @return The closest beat snap divisor to [time].
     */
    fun getClosestBeatDivisor(time: Double): Int {
        val timingPoint = timing.controlPointAt(time)

        var closestDivisor = 0
        var closestTime = Double.MAX_VALUE

        for (divisor in PREDEFINED_DIVISORS) {
            val distanceFromSnap = abs(time - getClosestSnappedTime(timingPoint, time, divisor))

            if (Precision.definitelyBigger(closestTime, distanceFromSnap)) {
                closestDivisor = divisor
                closestTime = distanceFromSnap
            }
        }

        return closestDivisor
    }

    companion object {
        /**
         * Beat snap divisors that are commonly used in beatmaps.
         */
        @JvmStatic
        val PREDEFINED_DIVISORS = intArrayOf(1, 2, 3, 4, 6, 8, 12, 16)

        private fun getClosestSnappedTime(timingPoint: TimingControlPoint, time: Double, beatDivisor: Int): Double {
            val beatLength = timingPoint.msPerBeat / beatDivisor
            val beats = ((max(time, 0.0) - timingPoint.time) / beatLength).roundToInt()

            val snappedTime = timingPoint.time + beats * beatLength

            return snappedTime + if (snappedTime >= 0) 0.0 else beatLength
        }
    }
}