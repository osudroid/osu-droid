package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.timings.TimingControlPoint
import org.junit.Assert
import org.junit.Test

class BeatmapControlPointsTest {
    @Test
    fun `Test closest beat divisor`() {
        BeatmapControlPoints().apply {
            timing.add(TimingControlPoint(1000.0, 500.0, 4))
            timing.add(TimingControlPoint(3250.0, 500.0, 4))
            timing.add(TimingControlPoint(5750.0, 500.0, 4))

            Assert.assertEquals(1, getClosestBeatDivisor(0.0))
            Assert.assertEquals(4, getClosestBeatDivisor(1125.0))
            Assert.assertEquals(1, getClosestBeatDivisor(1500.0))
            Assert.assertEquals(2, getClosestBeatDivisor(3500.0))
            Assert.assertEquals(1, getClosestBeatDivisor(4250.0))
            Assert.assertEquals(2, getClosestBeatDivisor(5000.0))
            Assert.assertEquals(2, getClosestBeatDivisor(6000.0))
            Assert.assertEquals(1, getClosestBeatDivisor(6250.0))
        }
    }
}