package com.rian.osu.math

import kotlin.math.sqrt
import org.junit.Assert
import org.junit.Test

class Vector2Test {
    @Test
    fun testMultiplication() {
        fun test(vec1: Vector2, vec2: Vector2, result: Vector2) {
            val finalVec = vec1 * vec2

            Assert.assertEquals(result.x, finalVec.x, 1e-2f)
            Assert.assertEquals(result.y, finalVec.y, 1e-2f)
        }

        test(Vector2(2), Vector2(4), Vector2(8))
        test(Vector2(3, 4), Vector2(6, 5), Vector2(18, 20))
        test(Vector2(2.5f, 10f), Vector2(8f, 7.5f), Vector2(20, 75))
    }

    @Test
    fun testDivision() {
        val vec = Vector2(10)

        Assert.assertThrows(ArithmeticException::class.java) { vec / 0 }
        Assert.assertEquals(vec / 1, vec)
        Assert.assertEquals(vec / 2, Vector2(5))
        Assert.assertEquals(vec / 0.5, Vector2(20))
        Assert.assertEquals(vec / 2.5, Vector2(4))
        Assert.assertEquals(vec / 25, Vector2(0.4f))
    }

    @Test
    fun testAddition() {
        fun test(vec1: Vector2, vec2: Vector2, result: Vector2) {
            val finalVec = vec1 + vec2

            Assert.assertEquals(result.x, finalVec.x, 1e-2f)
            Assert.assertEquals(result.y, finalVec.y, 1e-2f)
        }

        test(Vector2(1), Vector2(2), Vector2(3))
        test(Vector2(5), Vector2(-1, 1), Vector2(4, 6))
        test(Vector2(12.5f, -5f), Vector2(-1.25f, 1.5f), Vector2(11.25f, -3.5f))
    }

    @Test
    fun testSubtraction() {
        fun test(vec1: Vector2, vec2: Vector2, result: Vector2) {
            val finalVec = vec1 - vec2

            Assert.assertEquals(result.x, finalVec.x, 1e-2f)
            Assert.assertEquals(result.y, finalVec.y, 1e-2f)
        }

        test(Vector2(1), Vector2(2), Vector2(-1))
        test(Vector2(5), Vector2(-1, 1), Vector2(6, 4))
        test(Vector2(12.5f, -5f), Vector2(-1.25f, 1.5f), Vector2(13.75f, -6.5f))
    }

    @Test
    fun testLength() {
        fun test(vec: Vector2, result: Float) = Assert.assertEquals(result, vec.length, 1e-2f)

        test(Vector2(1, 1), sqrt(2f))
        test(Vector2(2, 1), sqrt(5f))
        test(Vector2(-4, 6), sqrt(52f))
        test(Vector2(13, -12), sqrt(313f))
        test(Vector2(-3, -5), sqrt(34f))
    }

    @Test
    fun testDotMultiplication() {
        fun test(vec1: Vector2, vec2: Vector2, result: Float) = Assert.assertEquals(result, vec1.dot(vec2), 1e-2f)

        test(Vector2(2), Vector2(4), 16f)
        test(Vector2(3, 2), Vector2(1, -3), -3f)
        test(Vector2(5, -2), Vector2(4, -4), 28f)
        test(Vector2(-0.5f, 4f), Vector2(2, 6), 23f)
        test(Vector2(-2, -5), Vector2(-4, 2), -2f)
    }

    @Test
    fun testScaling() {
        val vec = Vector2(10)

        Assert.assertEquals(vec * 1, vec)
        Assert.assertEquals(vec * 0.5f, Vector2(5))
        Assert.assertEquals(vec * 2.5f, Vector2(25))
    }

    @Test
    fun testDistance() {
        fun test(vec1: Vector2, vec2: Vector2, result: Float) =
            Assert.assertEquals(result, vec1.getDistance(vec2), 1e-2f)

        test(Vector2(1), Vector2(2), sqrt(2f))
        test(Vector2(5), Vector2(-1, 1), sqrt(52f))
        test(Vector2(12.5f, -5f), Vector2(-1.25f, 1.5f), sqrt(231.3125f))
    }

    @Test
    fun testNormalization() {
        fun test(vec: Vector2, result: Vector2) {
            vec.normalize()

            Assert.assertEquals(result.x, vec.x, 1e-2f)
            Assert.assertEquals(result.y, vec.y, 1e-2f)
        }

        test(Vector2(10), Vector2(0.71f))
        test(Vector2(15), Vector2(0.71f))
        test(Vector2(10, 20), Vector2(0.45f, 0.89f))
    }

    @Test
    fun testEquality() {
        Assert.assertEquals(Vector2(10), Vector2(10))
        Assert.assertNotEquals(Vector2(10), Vector2(10, 15))
    }
}