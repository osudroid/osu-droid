package com.osudroid.math

import com.osudroid.math.Interpolation
import com.osudroid.math.Vector2
import org.junit.Assert
import org.junit.Test

class InterpolationTest {
    @Test
    fun `Test float linear interpolation`() {
        data class Case(val start: Float, val end: Float, val amount: Float, val expected: Float)

        listOf(
            Case(4f, 5f, 2f, 6f),
            Case(2f, 5f, 2f, 8f),
            Case(-1f, 7f, 2f, 15f)
        ).forEach { (start, end, amount, expected) ->
            Assert.assertEquals(
                "Invalid linear interpolation result for start=$start, end=$end, amount=$amount",
                expected,
                Interpolation.linear(start, end, amount)
            )
        }
    }

    @Test
    fun `Test double linear interpolation`() {
        data class Case(val start: Double, val end: Double, val amount: Double, val expected: Double)

        listOf(
            Case(4.0, 5.0, 2.0, 6.0),
            Case(2.0, 5.0, 2.0, 8.0),
            Case(-1.0, 7.0, 2.0, 15.0)
        ).forEach { (start, end, amount, expected) ->
            Assert.assertEquals(
                "Invalid linear interpolation result for start=$start, end=$end, amount=$amount",
                expected,
                Interpolation.linear(start, end, amount),
                1e-9
            )
        }
    }

    @Test
    fun `Test vector linear interpolation`() {
        data class Case(val start: Vector2, val end: Vector2, val amount: Float, val expected: Vector2)

        listOf(
            Case(Vector2(4), Vector2(5), 2f, Vector2(6)),
            Case(Vector2(2, 3), Vector2(5, 6), 2f, Vector2(8, 9)),
            Case(Vector2(-1, 2), Vector2(7, 8), 2f, Vector2(15, 14))
        ).forEach { (start, end, amount, expected) ->
            Assert.assertEquals(
                "Invalid linear interpolation result for start=$start, end=$end, amount=$amount",
                expected,
                Interpolation.linear(start, end, amount)
            )
        }
    }

    @Test
    fun `Test reverse linear interpolation`() {
        data class Case(val x: Double, val start: Double, val end: Double, val expected: Double)

        listOf(
            Case(5.0, 4.0, 6.0, 0.5),
            Case(5.0, 2.0, 8.0, 0.5),
            Case(7.0, -1.0, 15.0, 0.5)
        ).forEach { (x, start, end, expected) ->
            Assert.assertEquals(
                "Invalid reverse linear interpolation result for x=$x, start=$start, end=$end",
                expected,
                Interpolation.reverseLinear(x, start, end),
                1e-2
            )
        }
    }
}