package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class InterpolationTest {
    @Test
    fun `Test number linear interpolation`() {
        Assert.assertEquals(Interpolation.linear(4.0, 5.0, 2.0), 6.0, 0.0)
        Assert.assertEquals(Interpolation.linear(2.0, 5.0, 2.0), 8.0, 0.0)
        Assert.assertEquals(Interpolation.linear(-1.0, 7.0, 2.0), 15.0, 0.0)
    }

    @Test
    fun `Test vector linear interpolation`() {
        Assert.assertEquals(Interpolation.linear(Vector2(4), Vector2(5), 2f), Vector2(6))
        Assert.assertEquals(Interpolation.linear(Vector2(2, 3), Vector2(5, 6), 2f), Vector2(8, 9))
        Assert.assertEquals(Interpolation.linear(Vector2(-1, 2), Vector2(7, 8), 2f), Vector2(15, 14))
    }

    @Test
    fun `Test reverse linear interpolation`() {
        Assert.assertEquals(Interpolation.reverseLinear(5.0, 4.0, 6.0), 0.5, 1e-2)
        Assert.assertEquals(Interpolation.reverseLinear(5.0, 2.0, 8.0), 0.5, 1e-2)
        Assert.assertEquals(Interpolation.reverseLinear(7.0, -1.0, 15.0), 0.5, 1e-2)
    }
}