package com.osudroid.beatmaps.timings

import com.osudroid.beatmaps.timings.TimingControlPoint
import org.junit.Assert
import org.junit.Test

class TimingControlPointTest {
    @Test
    fun `Test redundancy`() {
        val controlPoint = TimingControlPoint(1000.0, 1000.0, 4)

        Assert.assertFalse(controlPoint.isRedundant(TimingControlPoint(1500.0, 1000.0, 4)))
        Assert.assertFalse(controlPoint.isRedundant(TimingControlPoint(1500.0, 500.0, 4)))
        Assert.assertFalse(controlPoint.isRedundant(TimingControlPoint(1500.0, 1000.0, 2)))
    }
}