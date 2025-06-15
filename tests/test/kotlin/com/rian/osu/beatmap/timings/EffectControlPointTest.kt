package com.rian.osu.beatmap.timings

import org.junit.Assert
import org.junit.Test

class EffectControlPointTest {
    @Test
    fun `Test redundancy`() {
        val controlPoint = EffectControlPoint(1000.0, false)

        Assert.assertTrue(controlPoint.isRedundant(EffectControlPoint(1500.0, false)))
        Assert.assertFalse(controlPoint.isRedundant(EffectControlPoint(1500.0, true)))
    }
}