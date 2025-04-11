package com.rian.osu.beatmap.timings

import org.junit.Assert
import org.junit.Test

class DifficultyControlPointTest {
    @Test
    fun testRedundancy() {
        val controlPoint = DifficultyControlPoint(1000.0, 1.0, true)

        Assert.assertTrue(controlPoint.isRedundant(DifficultyControlPoint(1500.0, 1.0, true)))
        Assert.assertFalse(controlPoint.isRedundant(DifficultyControlPoint(1500.0, 1.25, true)))
        Assert.assertTrue(controlPoint.isRedundant(DifficultyControlPoint(1500.0, 1.0, false)))
    }
}