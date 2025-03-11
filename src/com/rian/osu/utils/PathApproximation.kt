package com.rian.osu.utils

import com.rian.osu.math.Precision.almostEquals
import com.rian.osu.math.Vector2
import com.rian.osu.math.times
import java.util.*
import kotlin.math.*

/**
 * Helper methods to approximate a path by interpolating a sequence of control points.
 */
object PathApproximation {
    /**
     * The amount of pieces to calculate for each control point quadruplet.
     */
    const val CATMULL_DETAIL = 50

    private const val BEZIER_TOLERANCE = 0.25f
    private const val CIRCULAR_ARC_TOLERANCE = 0.1f

    /**
     * Creates a piecewise-linear approximation of a Bézier curve by adaptively repeatedly subdividing
     * the control points until their approximation error vanishes below a given threshold.
     *
     * @param controlPoints The control points of the curve.
     */
    fun approximateBezier(controlPoints: List<Vector2>): MutableList<Vector2> {
        val output = mutableListOf<Vector2>()
        val count = controlPoints.size - 1

        if (count < 0) {
            return output
        }

        // "toFlatten" contains all the curves which are not yet approximated well enough.
        // We use a stack to emulate recursion without the risk of running into a stack overflow.
        // (More specifically, we iteratively and adaptively refine our curve with a
        // depth-first search (https://en.wikipedia.org/wiki/Depth-first_search)
        // over the tree resulting from the subdivisions we make.)
        val toFlatten = Stack<Array<Vector2?>>()
        val freeBuffers = Stack<Array<Vector2?>>()

        toFlatten.push(controlPoints.toTypedArray())
        val subdivisionBuffer1 = arrayOfNulls<Vector2>(count + 1)
        val subdivisionBuffer2 = arrayOfNulls<Vector2>(count * 2 + 1)

        while (toFlatten.isNotEmpty()) {
            val parent = toFlatten.pop()

            if (bezierIsFlatEnough(parent)) {
                // If the control points we currently operate on are sufficiently "flat", we use
                // an extension to De Casteljau's algorithm to obtain a piecewise-linear approximation
                // of the Bézier curve represented by our control points, consisting of the same amount
                // of points as there are control points.
                bezierApproximate(parent, output, subdivisionBuffer1, subdivisionBuffer2, count + 1)
                freeBuffers.push(parent)
                continue
            }

            // If we do not yet have a sufficiently "flat" (in other words, detailed) approximation we keep
            // subdividing the curve we are currently operating on.
            val rightChild = if (freeBuffers.isNotEmpty()) freeBuffers.pop() else arrayOfNulls(count + 1)
            bezierSubdivide(parent, subdivisionBuffer2, rightChild, subdivisionBuffer1, count + 1)

            // We re-use the buffer of the parent for one of the children, so that we save one allocation per iteration.
            if (count + 1 >= 0) {
                System.arraycopy(subdivisionBuffer2, 0, parent, 0, count + 1)
            }

            toFlatten.push(rightChild)
            toFlatten.push(parent)
        }
        output.add(controlPoints[count])
        return output
    }

    /**
     * Creates a piecewise-linear approximation of a Catmull-Rom spline.
     *
     * @param controlPoints The control points.
     */
    fun approximateCatmull(controlPoints: List<Vector2>): MutableList<Vector2> {
        val result = mutableListOf<Vector2>()

        for (i in 0 until controlPoints.size - 1) {
            val v1 = if (i > 0) controlPoints[i - 1] else controlPoints[i]
            val v2 = controlPoints[i]
            val v3 = if (i < controlPoints.size - 1) controlPoints[i + 1] else v2 + v2 - v1
            val v4 = if (i < controlPoints.size - 2) controlPoints[i + 2] else v3 + v3 - v2

            for (c in 0 until CATMULL_DETAIL) {
                result.add(catmullFindPoint(v1, v2, v3, v4, c.toFloat() / CATMULL_DETAIL))
                result.add(catmullFindPoint(v1, v2, v3, v4, (c + 1).toFloat() / CATMULL_DETAIL))
            }
        }

        return result
    }

    /**
     * Creates a piecewise-linear approximation of a circular arc curve.
     *
     * @param controlPoints The control points.
     */
    fun approximateCircularArc(controlPoints: List<Vector2>): MutableList<Vector2> {
        if (controlPoints.size != 3) {
            return approximateBezier(controlPoints)
        }

        val a = controlPoints[0]
        val b = controlPoints[1]
        val c = controlPoints[2]

        // If we have a degenerate triangle where a side-length is almost zero, then give up and fall
        // back to a more numerically stable method.
        if (almostEquals(0f, (b.y - a.y) * (c.x - a.x) - (b.x - a.x) * (c.y - a.y))) {
            return approximateBezier(controlPoints)
        }

        // See: https://en.wikipedia.org/wiki/Circumscribed_circle#Cartesian_coordinates_2
        val d = 2 * (a.x * (b -c).y + b.x * (c - a).y + c.x * (a - b).y)
        val aSq = a.lengthSquared
        val bSq = b.lengthSquared
        val cSq = c.lengthSquared

        val center: Vector2 = Vector2(
            aSq * (b - c).y + bSq * (c - a).y + cSq * (a - b).y,
            aSq * (c - b).x + bSq * (a - c).x + cSq * (b - a).x
        ) / d

        val dA = a - center
        val dC = c - center

        val radius = dA.length
        val thetaStart = atan2(dA.y.toDouble(), dA.x.toDouble())
        var thetaEnd = atan2(dC.y.toDouble(), dC.x.toDouble())

        while (thetaEnd < thetaStart) {
            thetaEnd += 2 * Math.PI
        }

        var direction = 1.0
        var thetaRange = thetaEnd - thetaStart

        // Decide in which direction to draw the circle, depending on which side of
        // AC B lies.
        var orthoAtoC = c - a
        orthoAtoC = Vector2(orthoAtoC.y, -orthoAtoC.x)

        if (orthoAtoC.dot(b - a) < 0) {
            direction = -direction
            thetaRange = 2 * Math.PI - thetaRange
        }

        // We select the amount of points for the approximation by requiring the discrete curvature
        // to be smaller than the provided tolerance. The exact angle required to meet the tolerance
        // is: 2 * acos(1 - TOLERANCE / radius)
        // The special case is required for extremely short sliders where the radius is smaller than
        // the tolerance. This is a pathological rather than a realistic case.
        val amountPoints =
            if (2 * radius <= CIRCULAR_ARC_TOLERANCE) 2
            else max(
                2,
                ceil(thetaRange / (2 * acos(1 - CIRCULAR_ARC_TOLERANCE / radius)))
                    .toInt()
            )

        val output = mutableListOf<Vector2>()

        for (i in 0 until amountPoints) {
            val fraction = i.toDouble() / (amountPoints - 1)
            val theta = thetaStart + direction * fraction * thetaRange
            val o = Vector2(cos(theta).toFloat(), sin(theta).toFloat()) * radius

            output.add(center + o)
        }

        return output
    }

    /**
     * Creates a piecewise-linear approximation of a linear curve.
     * Basically, returns the input.
     *
     * @param controlPoints The control points.
     */
    fun approximateLinear(controlPoints: List<Vector2>) = controlPoints

    /**
     * Checks if a Bézier curve is flat enough to be approximated.
     *
     * Make sure the 2nd order derivative (approximated using finite elements) is within tolerable bounds.
     *
     * NOTE: The 2nd order derivative of a 2D curve represents its curvature, so intuitively this function
     * checks (as the name suggests) whether our approximation is *locally* "flat". More curvy parts
     * need to have a denser approximation to be more "flat".
     *
     * @param controlPoints The control points.
     */
    private fun bezierIsFlatEnough(controlPoints: Array<Vector2?>) =
        controlPoints.let {
            for (i in 1 until it.size - 1) {
                val prev = it[i - 1]!!
                val current = it[i]!!
                val next = it[i + 1]!!
                val finalVec = prev - current * 2 + next

                if (finalVec.length.pow(2f) > BEZIER_TOLERANCE.pow(2f) * 4) {
                    return@let false
                }
            }

            true
        }

    /**
     * Approximates a Bézier curve.
     *
     * This uses [De Casteljau's algorithm](https://en.wikipedia.org/wiki/De_Casteljau%27s_algorithm) to obtain an optimal
     * piecewise-linear approximation of the Bézier curve with the same amount of points as there are control points.
     *
     * @param controlPoints The control points describing the Bézier curve to be approximated.
     * @param output The points representing the resulting piecewise-linear approximation.
     * @param subdivisionBuffer1 The first buffer containing the current subdivision state.
     * @param subdivisionBuffer2 The second buffer containing the current subdivision state.
     * @param count The number of control points in the original array.
     */
    private fun bezierApproximate(
        controlPoints: Array<Vector2?>, output: MutableList<Vector2>,
        subdivisionBuffer1: Array<Vector2?>, subdivisionBuffer2: Array<Vector2?>,
        count: Int
    ) {
        bezierSubdivide(controlPoints, subdivisionBuffer2, subdivisionBuffer1, subdivisionBuffer1, count)

        for (i in 0 until count - 1) {
            subdivisionBuffer2[count + i] = subdivisionBuffer1[i + 1]
        }

        output.add(controlPoints[0]!!)

        for (i in 1 until count - 1) {
            val index = 2 * i
            val p = 0.25 * (subdivisionBuffer2[index - 1]!! + subdivisionBuffer2[index]!! * 2 + subdivisionBuffer2[index + 1]!!)

            output.add(p)
        }
    }

    /**
     * Subdivides `n` control points representing a Bézier curve into 2 sets of `n`
     * control points, each describing a Bézier curve equivalent to a half of the original curve.
     * Effectively this splits the original curve into 2 curves which result in the original curve
     * when pieced back together.
     *
     * @param controlPoints The anchor points of the slider.
     * @param l Parts of the slider for approximation.
     * @param r Parts of the slider for approximation.
     * @param subdivisionBuffer Parts of the slider for approximation.
     * @param count The amount of anchor points in the slider.
     */
    private fun bezierSubdivide(
        controlPoints: Array<Vector2?>, l: Array<Vector2?>, r: Array<Vector2?>,
        subdivisionBuffer: Array<Vector2?>, count: Int
    ) {
        for (i in 0 until count) {
            subdivisionBuffer[i] = controlPoints[i]
        }

        for (i in 0 until count) {
            l[i] = subdivisionBuffer[0]
            r[count - i - 1] = subdivisionBuffer[count - i - 1]

            for (j in 0 until count - i - 1) {
                subdivisionBuffer[j] = (subdivisionBuffer[j]!! + subdivisionBuffer[j + 1]!!) / 2
            }
        }
    }

    /**
     * Finds a point on the spline at the position of a parameter.
     *
     * @param vec1 The first [Vector2].
     * @param vec2 The second [Vector2].
     * @param vec3 The third [Vector2].
     * @param vec4 The fourth [Vector2].
     * @param t The parameter at which to find the point on the spline, in the range `[0, 1]`.
     */
    private fun catmullFindPoint(
        vec1: Vector2, vec2: Vector2,
        vec3: Vector2, vec4: Vector2, t: Float
    ): Vector2 {
        val t2 = t.pow(2f)
        val t3 = t.pow(3f)

        return Vector2(
            0.5f *
                    (2 * vec2.x + (-vec1.x + vec3.x) * t + (2 * vec1.x - 5 * vec2.x + 4 * vec3.x - vec4.x) * t2 + (-vec1.x + 3 * vec2.x - 3 * vec3.x + vec4.x) * t3),
            0.5f *
                    (2 * vec2.y + (-vec1.y + vec3.y) * t + (2 * vec1.y - 5 * vec2.y + 4 * vec3.y - vec4.y) * t2 + (-vec1.y + 3 * vec2.y - 3 * vec3.y + vec4.y) * t3)
        )
    }
}
