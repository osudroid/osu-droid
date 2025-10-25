package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class InterpolationTest {
    @Test
    fun `Test number linear interpolation`() {
        Assert.assertEquals(6.0, Interpolation.linear(4.0, 5.0, 2.0), 0.0)
        Assert.assertEquals(8.0, Interpolation.linear(2.0, 5.0, 2.0), 0.0)
        Assert.assertEquals(15.0, Interpolation.linear(-1.0, 7.0, 2.0), 0.0)
    }

    @Test
    fun `Test vector linear interpolation`() {
        Assert.assertEquals(Vector2(6), Interpolation.linear(Vector2(4), Vector2(5), 2f))
        Assert.assertEquals(Vector2(8, 9), Interpolation.linear(Vector2(2, 3), Vector2(5, 6), 2f))
        Assert.assertEquals(Vector2(15, 14), Interpolation.linear(Vector2(-1, 2), Vector2(7, 8), 2f))
    }

    @Test
    fun `Test reverse linear interpolation`() {
        Assert.assertEquals(0.5, Interpolation.reverseLinear(5.0, 4.0, 6.0), 1e-2)
        Assert.assertEquals(0.5, Interpolation.reverseLinear(5.0, 2.0, 8.0), 1e-2)
        Assert.assertEquals(0.5, Interpolation.reverseLinear(7.0, -1.0, 15.0), 1e-2)
    }
}