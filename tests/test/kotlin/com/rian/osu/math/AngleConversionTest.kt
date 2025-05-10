package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class AngleConversionTest {
    @Test
    fun `Test degrees to radians conversion`() {
        Assert.assertEquals(0f, 0.toRadians(), 1e-5f)
        Assert.assertEquals(0.0, 0L.toRadians(), 1e-5)
        Assert.assertEquals(0.0, 0.0.toRadians(), 1e-5)
        Assert.assertEquals(0f, 0f.toRadians(), 1e-5f)

        Assert.assertEquals(Math.PI.toFloat() / 4, 45.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI.toFloat() / 4, 45f.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI / 4, 45L.toRadians(), 1e-5)
        Assert.assertEquals(Math.PI / 4, 45.0.toRadians(), 1e-5)

        Assert.assertEquals(Math.PI.toFloat() / 3, 60.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI.toFloat() / 3, 60f.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI / 3, 60L.toRadians(), 1e-5)
        Assert.assertEquals(Math.PI / 3, 60.0.toRadians(), 1e-5)

        Assert.assertEquals(Math.PI.toFloat() / 2, 90.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI.toFloat() / 2, 90f.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI / 2, 90L.toRadians(), 1e-5)
        Assert.assertEquals(Math.PI / 2, 90.0.toRadians(), 1e-5)

        Assert.assertEquals(Math.PI.toFloat(), 180.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI.toFloat(), 180f.toRadians(), 1e-5f)
        Assert.assertEquals(Math.PI, 180L.toRadians(), 1e-5)
        Assert.assertEquals(Math.PI, 180.0.toRadians(), 1e-5)
    }

    @Test
    fun `Test radians to degrees conversion`() {
        Assert.assertEquals(0f, 0.toDegrees(), 1e-5f)
        Assert.assertEquals(0.0, 0L.toDegrees(), 1e-5)
        Assert.assertEquals(0.0, 0.0.toDegrees(), 1e-5)
        Assert.assertEquals(0f, 0f.toDegrees(), 1e-5f)

        Assert.assertEquals(45f, (Math.PI / 4).toFloat().toDegrees(), 1e-5f)
        Assert.assertEquals(45.0, (Math.PI / 4).toDegrees(), 1e-5)

        Assert.assertEquals(60f, (Math.PI / 3).toFloat().toDegrees(), 1e-5f)
        Assert.assertEquals(60.0, (Math.PI / 3).toDegrees(), 1e-5)

        Assert.assertEquals(90f, (Math.PI / 2).toFloat().toDegrees(), 1e-5f)
        Assert.assertEquals(90.0, (Math.PI / 2).toDegrees(), 1e-5)

        Assert.assertEquals(180f, Math.PI.toFloat().toDegrees(), 1e-5f)
        Assert.assertEquals(180.0, Math.PI.toDegrees(), 1e-5)
    }
}