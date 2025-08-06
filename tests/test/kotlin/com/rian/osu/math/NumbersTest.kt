package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class NumbersTest {
    @Test
    fun `Test float precise rounding`() {
        fun test(expected: Float, value: Float, precision: Int) =
            Assert.assertEquals(expected, value.preciseRoundBy(precision), 0f)

        Assert.assertThrows(IllegalArgumentException::class.java) { 0.1f.preciseRoundBy(-1) }

        test(Float.NaN, Float.NaN, 0)
        test(Float.POSITIVE_INFINITY, Float.MAX_VALUE, 0)
        test(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, 0)
        test(0f, 0.12345678f, 0)
        test(0.1f, 0.12345678f, 1)
        test(0.12f, 0.12345678f, 2)
        test(0.123f, 0.12345678f, 3)
        test(0.1235f, 0.12345678f, 4)
        test(0.12346f, 0.12345678f, 5)
        test(0.123457f, 0.12345678f, 6)
        test(0.1234568f, 0.12345678f, 7)
        test(0.12345678f, 0.12345678f, 8)
    }

    @Test
    fun `Test double precise rounding`() {
        fun test(expected: Double, value: Double, precision: Int) =
            Assert.assertEquals(expected, value.preciseRoundBy(precision), 0.0)

        Assert.assertThrows(IllegalArgumentException::class.java) { 0.1.preciseRoundBy(-1) }

        test(Double.NaN, Double.NaN, 0)
        test(Double.POSITIVE_INFINITY, Double.MAX_VALUE, 0)
        test(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, 0)
        test(0.0, 0.123456789, 0)
        test(0.1, 0.123456789, 1)
        test(0.12, 0.123456789, 2)
        test(0.123, 0.123456789, 3)
        test(0.1235, 0.123456789, 4)
        test(0.12346, 0.123456789, 5)
        test(0.123457, 0.123456789, 6)
        test(0.1234568, 0.123456789, 7)
        test(0.12345679, 0.123456789, 8)
        test(0.123456789, 0.123456789, 9)
    }
}