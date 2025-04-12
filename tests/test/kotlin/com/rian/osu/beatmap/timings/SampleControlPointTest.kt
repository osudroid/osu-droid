package com.rian.osu.beatmap.timings

import com.rian.osu.beatmap.constants.SampleBank
import org.junit.Assert
import org.junit.Test

class SampleControlPointTest {
    @Test
    fun testRedundancy() {
        val controlPoint = SampleControlPoint(1000.0, SampleBank.Normal, 0, 0)

        Assert.assertTrue(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Normal, 0, 0)))
        Assert.assertFalse(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Drum, 0, 0)))
        Assert.assertFalse(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Normal, 20, 0)))
        Assert.assertFalse(controlPoint.isRedundant(SampleControlPoint(1500.0, SampleBank.Normal, 0, 1)))
    }
}