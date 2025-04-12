package com.rian.osu.utils

import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class PathApproximationTest {
    @Test
    fun testLinearApproximation() {
        val controlPoints = listOf(Vector2(0), Vector2(100, 0), Vector2(200, 0))

        Assert.assertArrayEquals(
            PathApproximation.approximateLinear(controlPoints).toTypedArray(),
            controlPoints.toTypedArray()
        )
    }

    @Test
    fun testCatmullApproximation() {
        val controlPoints = listOf(Vector2(0), Vector2(-29, -90), Vector2(96, -224))
        val approximatedControlPoints = PathApproximation.approximateCatmull(controlPoints)

        for (controlPoint in controlPoints) {
            Assert.assertTrue(controlPoint in approximatedControlPoints)
        }

        testControlPointsValidity(approximatedControlPoints)
    }

    @Test
    fun testPerfectCurveApproximationSmallSideTriangle() {
        val controlPoints = listOf(Vector2(0), Vector2(1e-5f, 0f), Vector2(0f, 1e-5f))
        val approximatedControlPoints = PathApproximation.approximateCatmull(controlPoints)

        testControlPointsValidity(approximatedControlPoints)
    }

    @Test
    fun testPerfectCurveApproximationRadiusSmallerThanTolerance() {
        val controlPoints = listOf(Vector2(0), Vector2(0.05f, 0f), Vector2(0f, 0.05f))
        val approximatedControlPoints = PathApproximation.approximateCatmull(controlPoints)

        testControlPointsValidity(approximatedControlPoints)
    }

    @Test
    fun testRegularPerfectCurveApproximation() {
        val controlPoints = listOf(Vector2(0), Vector2(-25, 25), Vector2(58, 39))
        val approximatedControlPoints = PathApproximation.approximateCatmull(controlPoints)

        testControlPointsValidity(approximatedControlPoints)
    }

    @Test
    fun testBezierCurveApproximation() {
        val controlPoints = listOf(Vector2(0), Vector2(-125, 44), Vector2(-88), Vector2(-234, -6))
        val approximatedControlPoints = PathApproximation.approximateBezier(controlPoints)

        testControlPointsValidity(approximatedControlPoints)
    }

    private fun testControlPointsValidity(controlPoints: Iterable<Vector2>) {
        for (controlPoint in controlPoints) {
            Assert.assertFalse(controlPoint.x.isNaN())
            Assert.assertFalse(controlPoint.y.isNaN())

            Assert.assertTrue(controlPoint.x.isFinite())
            Assert.assertTrue(controlPoint.y.isFinite())
        }
    }
}