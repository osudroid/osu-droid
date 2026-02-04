package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class PolynomialTest {
    @Test
    fun `Test polynomial`() {
        Assert.assertEquals(0.0, Polynomial.evaluate(0.0, doubleArrayOf()), 0.0)
        Assert.assertEquals(0.0, Polynomial.evaluate(123.0, doubleArrayOf()), 0.0)
        Assert.assertEquals(0.0, Polynomial.evaluate(0.0, doubleArrayOf(0.0)), 0.0)
        Assert.assertEquals(0.0, Polynomial.evaluate(123.0, doubleArrayOf(0.0)), 0.0)
        Assert.assertEquals(1.0, Polynomial.evaluate(0.0, doubleArrayOf(1.0)), 0.0)
        Assert.assertEquals(1.0, Polynomial.evaluate(123.0, doubleArrayOf(1.0)), 0.0)
        Assert.assertEquals(2.0, Polynomial.evaluate(0.0, doubleArrayOf(2.0)), 0.0)
        Assert.assertEquals(2.0, Polynomial.evaluate(123.0, doubleArrayOf(2.0)), 0.0)
        Assert.assertEquals(1.0, Polynomial.evaluate(0.0, doubleArrayOf(1.0, 2.0)), 0.0)
        Assert.assertEquals(7.0, Polynomial.evaluate(3.0, doubleArrayOf(1.0, 2.0)), 0.0)
        Assert.assertEquals(1.0, Polynomial.evaluate(0.0, doubleArrayOf(1.0, 2.0, 3.0)), 0.0)
        Assert.assertEquals(57.0, Polynomial.evaluate(4.0, doubleArrayOf(1.0, 2.0, 3.0)), 0.0)
    }
}