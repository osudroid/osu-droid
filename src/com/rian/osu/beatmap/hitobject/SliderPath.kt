package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Precision.almostEquals
import com.rian.osu.math.Vector2
import com.rian.osu.utils.PathApproximation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the path of a [Slider].
 */
class SliderPath @JvmOverloads constructor(
    /**
     * The path type of the [Slider].
     */
    val pathType: SliderPathType,

    /**
     * The control points (anchor points) of this [SliderPath].
     */
    val controlPoints: List<Vector2>,

    /**
     * The distance that is expected when calculating [SliderPath].
     */
    val expectedDistance: Double,

    /**
     * The [CoroutineScope] to use for job cancellation.
     */
    scope: CoroutineScope? = null
) {
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
        calculatePath(scope)
        calculateCumulativeLength(scope)
    }

    /**
     * Computes the position on the [Slider] at a given progress that ranges from 0
     * (beginning of the path) to 1 (end of the path).
     *
     * @param progress Ranges from 0 (beginning of the path) to 1 (end of the path).
     */
    fun positionAt(progress: Double) =
        progressToDistance(progress).let { interpolateVertices(indexOfDistance(it), it) }

    /**
     * Computes the slider path until a given progress that ranges from 0 (beginning of the slider) to 1 (end of the
     * slider).
     *
     * @param p0 Start progress. Ranges from 0 (beginning of the slider) to 1 (end of the slider).
     * @param p1 End progress. Ranges from 0 (beginning of the slider) to 1 (end of the slider).
     * @param scope The [CoroutineScope] to use for job cancellation.
     * @return The computed path between the two ranges.
     */
    @JvmOverloads
    fun getPathToProgress(p0: Double, p1: Double, scope: CoroutineScope? = null): MutableList<Vector2> {
        val path = mutableListOf<Vector2>()
        val d0 = progressToDistance(p0)
        val d1 = progressToDistance(p1)

        var i = 0

        while (i < calculatedPath.size && cumulativeLength[i] < d0) {
            scope?.ensureActive()
            i++
        }

        path.add(interpolateVertices(i, d0))

        while (i < calculatedPath.size && cumulativeLength[i] <= d1) {
            scope?.ensureActive()
            path.add(calculatedPath[i++])
        }

        path.add(interpolateVertices(i, d1))

        return path
    }

    /**
     * Calculates the path of this [SliderPath].
     */
    private fun calculatePath(scope: CoroutineScope?) {
        calculatedPath.clear()

        if (controlPoints.isEmpty()) {
            return
        }

        calculatedPath.add(controlPoints[0])
        var spanStart = 0

        for (i in controlPoints.indices) {
            scope?.ensureActive()

            if (i == controlPoints.size - 1 || controlPoints[i] == controlPoints[i + 1]) {
                val spanEnd = i + 1
                val cpSpan = controlPoints.subList(spanStart, spanEnd)

                for (t in calculateSubPath(cpSpan, scope)) {
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
    private fun calculateCumulativeLength(scope: CoroutineScope?) {
        cumulativeLength.clear()
        cumulativeLength.add(0.0)

        var calculatedLength = 0.0

        for (i in 0 until calculatedPath.size - 1) {
            scope?.ensureActive()

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
                while (cumulativeLength.isNotEmpty() && cumulativeLength[cumulativeLength.size - 1] >= expectedDistance) {
                    scope?.ensureActive()
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

    private fun calculateSubPath(subControlPoints: List<Vector2>, scope: CoroutineScope?) =
        when (pathType) {
            SliderPathType.Linear -> PathApproximation.approximateLinear(subControlPoints)

            SliderPathType.PerfectCurve ->
                if (subControlPoints.size == 3) PathApproximation.approximateCircularArc(subControlPoints, scope)
                else PathApproximation.approximateBezier(subControlPoints, scope)

            SliderPathType.Catmull -> PathApproximation.approximateCatmull(subControlPoints, scope)

            else -> PathApproximation.approximateBezier(subControlPoints, scope)
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
