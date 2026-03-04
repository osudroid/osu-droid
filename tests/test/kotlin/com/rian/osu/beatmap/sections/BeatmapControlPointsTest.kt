package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.timings.TimingControlPoint
import org.junit.Assert
import org.junit.Test

class BeatmapControlPointsTest {
    @Test
    fun `Test closest beat divisor`() {
        data class Case(val beatDivisor: Int, val time: Double)

        val controlPoints = BeatmapControlPoints().apply {
            timing.add(TimingControlPoint(1000.0, 500.0, 4))
            timing.add(TimingControlPoint(3250.0, 500.0, 4))
            timing.add(TimingControlPoint(5750.0, 500.0, 4))
        }

        listOf(
            Case(1, 0.0),
            Case(4, 1125.0),
            Case(1, 1500.0),
            Case(2, 3500.0),
            Case(1, 4250.0),
            Case(2, 5000.0),
            Case(2, 6000.0),
            Case(1, 6250.0)
        ).forEach { (beatDivisor, time) ->
            Assert.assertEquals("Invalid beat divisor for $time", beatDivisor, controlPoints.getClosestBeatDivisor(time))
        }
    }
}