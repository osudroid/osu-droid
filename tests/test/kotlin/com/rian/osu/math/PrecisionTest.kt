package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class PrecisionTest {
    @Test
    fun testFloatPrecision() {
        Assert.assertFalse(Precision.almostEquals(1f, 2f))
        Assert.assertFalse(Precision.almostEquals(1f, 1.01f))
        Assert.assertFalse(Precision.almostEquals(1f, 1.001f))
        Assert.assertTrue(Precision.almostEquals(1f, 1.0001f))
        Assert.assertFalse(Precision.almostEquals(1f, 1.0001f, 1e-4f))
    }

    @Test
    fun testDoublePrecision() {
        Assert.assertFalse(Precision.almostEquals(1.0, 2.0))
        Assert.assertFalse(Precision.almostEquals(1.0, 1.0001))
        Assert.assertFalse(Precision.almostEquals(1.0, 1.000001))
        Assert.assertTrue(Precision.almostEquals(1.0, 1.00000001))
        Assert.assertFalse(Precision.almostEquals(1.0, 1.0000001, 1e-8))
    }
}