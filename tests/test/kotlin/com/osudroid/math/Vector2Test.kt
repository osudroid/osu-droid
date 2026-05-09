package com.osudroid.math

import com.osudroid.math.Vector2
import com.osudroid.math.times
import com.osudroid.math.toVector2
import kotlin.math.sqrt
import org.junit.Assert
import org.junit.Test

class Vector2Test {
    private fun assertVectorEquals(expected: Vector2, actual: Vector2, context: String = "", delta: Float = 1e-2f) {
        val message = if (context.isNotEmpty()) "Vectors differ in $context | " else ""

        Assert.assertEquals("${message}X mismatch", expected.x, actual.x, delta)
        Assert.assertEquals("${message}Y mismatch", expected.y, actual.y, delta)
    }

    @Test
    fun `Test multiplication`() {
        data class Case(val v1: Vector2, val v2: Vector2, val result: Vector2)

        listOf(
            Case(Vector2(2), Vector2(4), Vector2(8)),
            Case(Vector2(3, 4), Vector2(6, 5), Vector2(18, 20)),
            Case(Vector2(2.5f, 10f), Vector2(8f, 7.5f), Vector2(20, 75))
        ).forEach { (v1, v2, result) ->
            assertVectorEquals(result, v1 * v2, "v1 * v2")
        }
    }

    @Test
    fun `Test division`() {
        val vec = Vector2(10)

        Assert.assertThrows(ArithmeticException::class.java) { vec / 0 }

        listOf(
            1f to vec,
            2f to Vector2(5),
            0.5f to Vector2(20),
            2.5f to Vector2(4),
            25f to Vector2(0.4f)
        ).forEach { (divisor, expected) ->
            assertVectorEquals(expected, vec / divisor, "vec / $divisor")
        }
    }

    @Test
    fun `Test addition`() {
        data class Case(val v1: Vector2, val v2: Vector2, val result: Vector2)

        listOf(
            Case(Vector2(1), Vector2(2), Vector2(3)),
            Case(Vector2(5), Vector2(-1, 1), Vector2(4, 6)),
            Case(Vector2(12.5f, -5f), Vector2(-1.25f, 1.5f), Vector2(11.25f, -3.5f)),
        ).forEach { (v1, v2, result) ->
            assertVectorEquals(result, v1 + v2, "v1 + v2")
        }
    }

    @Test
    fun `Test subtraction`() {
        data class Case(val v1: Vector2, val v2: Vector2, val result: Vector2)

        listOf(
            Case(Vector2(5), Vector2(-1, 1), Vector2(6, 4)),
            Case(Vector2(1), Vector2(2), Vector2(-1)),
            Case(Vector2(12.5f, -5f), Vector2(-1.25f, 1.5f), Vector2(13.75f, -6.5f))
        ).forEach { (v1, v2, result) ->
            assertVectorEquals(result, v1 - v2, "v1 - v2")
        }
    }

    @Test
    fun `Test length`() {
        listOf(
            Vector2(1, 1) to sqrt(2f),
            Vector2(2, 1) to sqrt(5f),
            Vector2(-4, 6) to sqrt(52f),
            Vector2(13, -12) to sqrt(313f),
            Vector2(-3, -5) to sqrt(34f)
        ).forEach { (vec, result) ->
            Assert.assertEquals(
                "Invalid length for $vec",
                result,
                vec.length,
                1e-2f
            )
        }
    }

    @Test
    fun `Test dot multiplication`() {
        data class Case(val v1: Vector2, val v2: Vector2, val result: Float)

        listOf(
            Case(Vector2(2), Vector2(4), 16f),
            Case(Vector2(3, 2), Vector2(1, -3), -3f),
            Case(Vector2(5, -2), Vector2(4, -4), 28f),
            Case(Vector2(-0.5f, 4f), Vector2(2, 6), 23f),
            Case(Vector2(-2, -5), Vector2(-4, 2), -2f)
        ).forEach { (v1, v2, result) ->
            Assert.assertEquals(
                "Invalid dot product for $v1 and $v2",
                result,
                v1.dot(v2),
                1e-2f
            )
        }
    }

    @Test
    fun `Test scaling`() {
        val vec = Vector2(10)

        listOf(
            1f to vec,
            0.5f to Vector2(5),
            2.5f to Vector2(25)
        ).forEach { (scalar, result) ->
            assertVectorEquals(result, vec * scalar, "vec * $scalar")
        }
    }

    @Test
    fun `Test distance`() {
        data class Case(val v1: Vector2, val v2: Vector2, val result: Float)

        listOf(
            Case(Vector2(1), Vector2(2), sqrt(2f)),
            Case(Vector2(5), Vector2(-1, 1), sqrt(52f)),
            Case(Vector2(12.5f, -5f), Vector2(-1.25f, 1.5f), sqrt(231.3125f))
        ).forEach { (v1, v2, result) ->
            Assert.assertEquals(
                "Invalid distance between $v1 and $v2",
                result,
                v1.getDistance(v2),
                1e-2f
            )
        }
    }

    @Test
    fun `Test normalization`() {
        listOf(
            Vector2(10) to Vector2(0.71f),
            Vector2(15) to Vector2(0.71f),
            Vector2(10, 20) to Vector2(0.45f, 0.89f)
        ).forEach { (vec, result) ->
            vec.normalize()
            assertVectorEquals(result, vec, "normalized $vec")
        }
    }

    @Test
    fun `Test equality`() {
        Assert.assertEquals(Vector2(10), Vector2(10))
        Assert.assertNotEquals(Vector2(10), Vector2(10, 15))
    }

    @Test
    fun `Test float to Vector2 conversion`() {
        Assert.assertEquals(10f.toVector2(), Vector2(10))
        Assert.assertEquals(20f.toVector2(), Vector2(20))
        Assert.assertEquals(25f.toVector2(), Vector2(25))
    }

    @Test
    fun `Test integer Vector2 multiplication`() {
        Assert.assertEquals(2 * Vector2(2), Vector2(4))
        Assert.assertEquals(3 * Vector2(3, 4), Vector2(9, 12))
        Assert.assertEquals(4 * Vector2(2.5f, 10f), Vector2(10, 40))
    }

    @Test
    fun `Test float Vector2 multiplication`() {
        Assert.assertEquals(2.5f * Vector2(2), Vector2(5))
        Assert.assertEquals(3.5f * Vector2(3, 4), Vector2(10.5f, 14f))
        Assert.assertEquals(4.5f * Vector2(2.5f, 10f), Vector2(11.25f, 45f))
    }

    @Test
    fun `Test double Vector2 multiplication`() {
        Assert.assertEquals(2.5 * Vector2(2), Vector2(5))
        Assert.assertEquals(3.5 * Vector2(3, 4), Vector2(10.5f, 14f))
        Assert.assertEquals(4.5 * Vector2(2.5f, 10f), Vector2(11.25f, 45f))
    }

    @Test
    fun `Test Pair to Vector2 conversion`() {
        Assert.assertEquals((10f to 20f).toVector2(), Vector2(10, 20))
        Assert.assertEquals((25f to 30f).toVector2(), Vector2(25, 30))
        Assert.assertEquals((35f to 40f).toVector2(), Vector2(35, 40))
    }
}