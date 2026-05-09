package com.osudroid.math

import com.osudroid.math.Precision
import org.junit.Assert
import org.junit.Test

class PrecisionTest {
    @Test
    fun `Test float precision`() {
        data class Case(val a: Float, val b: Float, val expected: Boolean, val epsilon: Float? = null)

        listOf(
            Case(1f, 2f, false),
            Case(1f, 1.01f, false),
            Case(1f, 1.001f, false),
            Case(1f, 1.0001f, true),
            Case(1f, 1.0001f, false, 1e-4f)
        ).forEach { (a, b, expected, epsilon) ->
            val result =
                if (epsilon != null) Precision.almostEquals(a, b, epsilon)
                else Precision.almostEquals(a, b)

            Assert.assertEquals("Invalid equality for $a and $b", expected, result)
        }
    }

    @Test
    fun `Test double precision`() {
        data class Case(val a: Double, val b: Double, val expected: Boolean, val epsilon: Double? = null)

        listOf(
            Case(1.0, 2.0, false),
            Case(1.0, 1.0001, false),
            Case(1.0, 1.000001, false),
            Case(1.0, 1.00000001, true),
            Case(1.0, 1.0000001, false, 1e-8)
        ).forEach { (a, b, expected, epsilon) ->
            val result =
                if (epsilon != null) Precision.almostEquals(a, b, epsilon)
                else Precision.almostEquals(a, b)

            Assert.assertEquals("Invalid equality for $a and $b", expected, result)
        }
    }

    @Test
    fun `Test float definitely bigger`() {
        data class Case(val a: Float, val b: Float, val expected: Boolean)

        listOf(
            Case(2f, 1f, true),
            Case(1.01f, 1f, true),
            Case(1.001f, 1f, false),
            Case(1.0001f, 1.0002f, false)
        ).forEach { (a, b, expected) ->
            Assert.assertEquals(
                "$a is not definitely bigger than $b",
                expected,
                Precision.definitelyBigger(a, b)
            )
        }
    }

    @Test
    fun `Test double definitely bigger`() {
        data class Case(val a: Double, val b: Double, val expected: Boolean)

        listOf(
            Case(2.0, 1.0, true),
            Case(1.0001, 1.0, true),
            Case(1.00001, 1.0, true),
            Case(1.0000001, 1.0, false),
            Case(1.00001, 1.00002, false)
        ).forEach { (a, b, expected) ->
            Assert.assertEquals(
                "$a is not definitely bigger than $b",
                expected,
                Precision.definitelyBigger(a, b)
            )
        }
    }
}