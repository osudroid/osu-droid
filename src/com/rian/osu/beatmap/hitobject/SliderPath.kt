package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Precision.almostEquals
import com.rian.osu.math.Vector2
import com.rian.osu.utils.PathApproximation

/**
 * Represents the path of a [Slider].
 */
class SliderPath(
    /**
     * The path type of the [Slider].
     */
    type: SliderPathType,

    /**
     * The control points (anchor points) of this [SliderPath].
     */
    controlPoints: MutableList<Vector2>,

    /**
     * The distance that is expected when calculating [SliderPath].
     */
    expectedDistance: Double
) {
    /**
     * The path type of the [Slider].
     */
    var pathType: SliderPathType = type
        private set

    /**
     * The control points (anchor points) of this [SliderPath].
     */
    var controlPoints = controlPoints
        private set

    /**
     * The distance that is expected when calculating this [SliderPath].
     */
    var expectedDistance = expectedDistance
        private set

    /**
     * The calculated path of this [SliderPath].
     */
    var calculatedPath = mutableListOf<Vector2>()
        private set

    /**
     * The cumulative length of this [SliderPath].
     */
    var cumulativeLength = mutableListOf<Double>()
        private set

    init {
        calculatePath()
        calculateCumulativeLength()
    }

    /**
     * Computes the position on the [Slider] at a given progress that ranges from 0
     * (beginning of the path) to 1 (end of the path).
     *
     * @param progress Ranges from 0 (beginning of the path) to 1 (end of the path).
     */
    fun positionAt(progress: Double): Vector2 =
        progressToDistance(progress).let { interpolateVertices(indexOfDistance(it), it) }

    /**
     * Calculates the path of this [SliderPath].
     */
    private fun calculatePath() {
        calculatedPath.clear()
        if (controlPoints.isEmpty()) {
            return
        }
        calculatedPath.add(controlPoints[0])
        var spanStart = 0
        for (i in controlPoints.indices) {
            if (i == controlPoints.size - 1 || controlPoints[i] == controlPoints[i + 1]) {
                val spanEnd = i + 1
                val cpSpan: List<Vector2> = controlPoints.subList(spanStart, spanEnd)
                for (t in calculateSubPath(cpSpan)) {
                    if (calculatedPath.isEmpty() || calculatedPath[calculatedPath.size - 1] != t) {
                        calculatedPath.add(t)
                    }
                }
                spanStart = spanEnd
            }
        }
    }

    /**
     * Calculates the cumulative length of this [SliderPath].
     */
    private fun calculateCumulativeLength() {
        cumulativeLength.clear()
        cumulativeLength.add(0.0)

        var calculatedLength = 0.0

        for (i in 0 until calculatedPath.size - 1) {
            val diff = calculatedPath[i + 1] - calculatedPath[i]
            calculatedLength += diff.length.toDouble()
            cumulativeLength.add(calculatedLength)
        }

        if (calculatedLength != expectedDistance) {
            // In osu-stable, if the last two control points of a slider are equal, extension is not performed.
            if (
                controlPoints.size >= 2 &&
                controlPoints[controlPoints.size - 1] == controlPoints[controlPoints.size - 2] &&
                expectedDistance > calculatedLength
            ) {
                return
            }

            // The last length is always incorrect.
            cumulativeLength.removeAt(cumulativeLength.size - 1)
            var pathEndIndex = calculatedPath.size - 1

            if (calculatedLength > expectedDistance) {
                // The path will be shortened further, in which case we should trim any more unnecessary lengths and their associated path segments
                while (cumulativeLength.size > 0 && cumulativeLength[cumulativeLength.size - 1] >= expectedDistance) {
                    cumulativeLength.removeAt(cumulativeLength.size - 1)
                    calculatedPath.removeAt(pathEndIndex--)
                }
            }

            if (pathEndIndex <= 0) {
                // The expected distance is negative or zero
                cumulativeLength.add(0.0)
                return
            }

            // The direction of the segment to shorten or lengthen
            val dir = calculatedPath[pathEndIndex] - calculatedPath[pathEndIndex - 1]
            dir.normalize()

            calculatedPath[pathEndIndex] = calculatedPath[pathEndIndex - 1] + dir * (expectedDistance - cumulativeLength[cumulativeLength.size - 1]).toFloat()
            cumulativeLength.add(expectedDistance)
        }
    }

    private fun calculateSubPath(subControlPoints: List<Vector2>) =
        when (pathType) {
            SliderPathType.Linear -> PathApproximation.approximateLinear(subControlPoints)

            SliderPathType.PerfectCurve ->
                if (subControlPoints.size == 3) PathApproximation.approximateCircularArc(subControlPoints)
                else PathApproximation.approximateBezier(subControlPoints)

            SliderPathType.Catmull -> PathApproximation.approximateCatmull(subControlPoints)

            else -> PathApproximation.approximateBezier(subControlPoints)
        }

    /**
     * Returns the progress of reaching expected distance.
     */
    private fun progressToDistance(progress: Double) = progress.coerceIn(0.0, 1.0) * expectedDistance

    /**
     * Interpolates vertices of the [SliderPath] at a certain point.
     */
    private fun interpolateVertices(i: Int, d: Double): Vector2 {
        if (calculatedPath.isEmpty()) {
            return Vector2(0f)
        }

        if (i <= 0) {
            return calculatedPath[0]
        }

        if (i >= calculatedPath.size) {
            return calculatedPath[calculatedPath.size - 1]
        }

        val p0 = calculatedPath[i - 1]
        val p1 = calculatedPath[i]
        val d0 = cumulativeLength[i - 1]
        val d1 = cumulativeLength[i]

        // Avoid division by and almost-zero number in case two points are extremely close to each other.
        if (almostEquals(d0, d1)) {
            return p0
        }

        val w = (d - d0) / (d1 - d0)
        return p0 + (p1 - p0) * w.toFloat()
    }

    /**
     * Binary searches the cumulative length array and returns the
     * index at which the index of the array is more than [d].
     *
     * @param d The distance to search.
     * @return The index.
     */
    private fun indexOfDistance(d: Double): Int {
        if (cumulativeLength.isEmpty() || d < cumulativeLength[0]) {
            return 0
        }

        if (d >= cumulativeLength[cumulativeLength.size - 1]) {
            return cumulativeLength.size
        }

        var l = 0
        var r = cumulativeLength.size - 2

        while (l <= r) {
            val pivot = l + (r - l shr 1)
            val length = cumulativeLength[pivot]

            when {
                length < d -> l = pivot + 1
                length > d -> r = pivot - 1
                else -> return pivot
            }
        }

        return l
    }
}
