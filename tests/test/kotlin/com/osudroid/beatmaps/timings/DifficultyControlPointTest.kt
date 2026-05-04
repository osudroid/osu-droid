package com.osudroid.beatmaps.timings

import com.osudroid.beatmaps.timings.DifficultyControlPoint
import org.junit.Assert
import org.junit.Test

class DifficultyControlPointTest {
    @Test
    fun `Test redundancy`() {
        val controlPoint = DifficultyControlPoint(1000.0, 1.0, true)

        Assert.assertTrue(controlPoint.isRedundant(DifficultyControlPoint(1500.0, 1.0, true)))
        Assert.assertFalse(controlPoint.isRedundant(DifficultyControlPoint(1500.0, 1.25, true)))
        Assert.assertFalse(controlPoint.isRedundant(DifficultyControlPoint(1500.0, 1.0, false)))
    }
}