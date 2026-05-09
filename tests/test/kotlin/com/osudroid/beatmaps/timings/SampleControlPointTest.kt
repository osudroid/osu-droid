package com.osudroid.beatmaps.timings

import com.osudroid.beatmaps.constants.SampleBank
import com.osudroid.beatmaps.timings.SampleControlPoint
import org.junit.Assert
import org.junit.Test

class SampleControlPointTest {
    @Test
    fun `Test redundancy`() {
        val controlPoint = SampleControlPoint(1000.0, SampleBank.Normal, 0, 0)

        Assert.assertTrue(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Normal, 0, 0)))
        Assert.assertFalse(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Drum, 0, 0)))
        Assert.assertFalse(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Normal, 20, 0)))
        Assert.assertFalse(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Normal, 0, 1)))
    }
}