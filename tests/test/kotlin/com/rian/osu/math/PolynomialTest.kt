package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class PolynomialTest {
    @Test
    fun testPolynomial() {
        Assert.assertEquals(Polynomial.evaluate(0.0, doubleArrayOf()), 0.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(123.0, doubleArrayOf()), 0.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(0.0, doubleArrayOf(0.0)), 0.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(123.0, doubleArrayOf(0.0)), 0.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(0.0, doubleArrayOf(1.0)), 1.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(123.0, doubleArrayOf(1.0)), 1.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(0.0, doubleArrayOf(2.0)), 2.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(123.0, doubleArrayOf(2.0)), 2.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(0.0, doubleArrayOf(1.0, 2.0)), 1.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(3.0, doubleArrayOf(1.0, 2.0)), 7.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(0.0, doubleArrayOf(1.0, 2.0, 3.0)), 1.0, 0.0)
        Assert.assertEquals(Polynomial.evaluate(4.0, doubleArrayOf(1.0, 2.0, 3.0)), 57.0, 0.0)
    }
}