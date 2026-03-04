package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class NumbersTest {
    @Test
    fun `Test float precise rounding`() {
        data class Case(val expected: Float, val value: Float, val precision: Int)

        Assert.assertThrows(IllegalArgumentException::class.java) { 0.1f.preciseRoundBy(-1) }

        listOf(
            Case(Float.NaN, Float.NaN, 0),
            Case(Float.POSITIVE_INFINITY, Float.MAX_VALUE, 0),
            Case(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, 0),
            Case(0f, 0.12345678f, 0),
            Case(0.1f, 0.12345678f, 1),
            Case(0.12f, 0.12345678f, 2),
            Case(0.123f, 0.12345678f, 3),
            Case(0.1235f, 0.12345678f, 4),
            Case(0.12346f, 0.12345678f, 5),
            Case(0.123457f, 0.12345678f, 6),
            Case(0.1234568f, 0.12345678f, 7),
            Case(0.12345678f, 0.12345678f, 8)
        ).forEach { (expected, value, precision) ->
            Assert.assertEquals(
                "Invalid precise rounding for $value with precision $precision",
                expected,
                value.preciseRoundBy(precision),
                0f
            )
        }
    }

    @Test
    fun `Test double precise rounding`() {
        data class Case(val expected: Double, val value: Double, val precision: Int)

        Assert.assertThrows(IllegalArgumentException::class.java) { 0.1.preciseRoundBy(-1) }

        listOf(
            Case(Double.NaN, Double.NaN, 0),
            Case(Double.POSITIVE_INFINITY, Double.MAX_VALUE, 0),
            Case(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, 0),
            Case(0.0, 0.123456789, 0),
            Case(0.1, 0.123456789, 1),
            Case(0.12, 0.123456789, 2),
            Case(0.123, 0.123456789, 3),
            Case(0.1235, 0.123456789, 4),
            Case(0.12346, 0.123456789, 5),
            Case(0.123457, 0.123456789, 6),
            Case(0.1234568, 0.123456789, 7),
            Case(0.12345679, 0.123456789, 8),
            Case(0.123456789, 0.123456789, 9)
        ).forEach { (expected, value, precision) ->
            Assert.assertEquals(
                "Invalid precise rounding for $value with precision $precision",
                expected,
                value.preciseRoundBy(precision),
                0.0
            )
        }
    }
}