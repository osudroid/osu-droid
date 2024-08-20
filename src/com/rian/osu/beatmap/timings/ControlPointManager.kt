package com.rian.osu.beatmap.timings

/**
 * A manager for a type of control point.
 */
abstract class ControlPointManager<T : ControlPoint>(
    /**
     * The default control point for this type.
     */
    defaultControlPoint: T
) {
    /**
     * The default control point for this type.
     */
    var defaultControlPoint = defaultControlPoint
        private set

    /**
     * The control points in this manager.
     */
    @JvmField
    protected var controlPoints = mutableListOf<T>()

    /**
     * Finds the control point that is active at a given time.
     *
     * @param time The time, in milliseconds.
     * @return The active control point at the given time.
     */
    abstract fun controlPointAt(time: Double): T

    /**
     * Adds a new control point.
     *
     * Note that the provided control point may not be added if the correct state is already present at the control point's time.
     *
     * Additionally, any control point that exists in the same time will be removed.
     *
     * @param controlPoint The control point to add.
     * @return Whether the control point was added.
     */
    fun add(controlPoint: T): Boolean {
        var existing = controlPointAt(controlPoint.time)
        if (controlPoint.isRedundant(existing)) {
            return false
        }

        // Remove the existing control point if the new control point overrides it at the same time.
        while (controlPoint.time == existing.time) {
            if (!remove(existing)) {
                break
            }

            existing = controlPointAt(controlPoint.time)
        }

        controlPoints.add(findInsertionIndex(controlPoint.time), controlPoint)

        return true
    }

    /**
     * Removes a control point.
     *
     * This method will remove the earliest control point in the array that is equal to the given control point.
     *
     * @param controlPoint The control point to remove.
     * @return Whether the control point was removed.
     */
    fun remove(controlPoint: T) = controlPoints.remove(controlPoint)

    /**
     * Removes a control point at an index.
     *
     * @param index The index of the control point to remove.
     * @return The control point that was removed, `null` if no control points were removed.
     */
    fun remove(index: Int) =
        if (index < 0 || index > controlPoints.size - 1) null
        else controlPoints.removeAt(index)

    /**
     * Clears all control points in this manager.
     */
    fun clear() = controlPoints.clear()

    /**
     * Gets the control points in this manager.
     */
    fun getControlPoints() = controlPoints.toList()

    /**
     * Binary searches one of the control point lists to find the active control point at the given time.
     *
     * Includes logic for returning the default control point when no matching point is found.
     *
     * @param time The time to find the control point at, in milliseconds.
     * @return The active control point at the given time, or the default control point if none found.
     */
    protected fun binarySearchWithFallback(time: Double, fallback: T = defaultControlPoint) =
        binarySearch(time) ?: fallback

    /**
     * Binary searches the control point list to find the active control point at the given time.
     *
     * @param time The time to find the control point at, in milliseconds.
     * @return The active control point at the given time, `null` if none found.
     */
    protected fun binarySearch(time: Double): T? {
        if (controlPoints.size == 0 || time < controlPoints[0].time) {
            return null
        }

        if (time >= controlPoints.last().time) {
            return controlPoints.last()
        }

        var l = 0
        var r = controlPoints.size - 2

        while (l <= r) {
            val pivot = l + (r - l shr 1)
            val controlPoint = controlPoints[pivot]

            when {
                controlPoint.time < time -> l = pivot + 1
                controlPoint.time > time -> r = pivot - 1
                else -> return controlPoint
            }
        }

        // l will be the first control point with time > controlPoint.time, but we want the one before it
        return controlPoints[l - 1]
    }

    /**
     * Finds the insertion index of a control point in a given time.
     *
     * @param time The start time of the control point, in milliseconds.
     */
    private fun findInsertionIndex(time: Double): Int {
        if (controlPoints.size == 0 || time < controlPoints[0].time) {
            return 0
        }

        if (time >= controlPoints.last().time) {
            return controlPoints.size
        }

        var l = 0
        var r = controlPoints.size - 2

        while (l <= r) {
            val pivot = l + (r - l shr 1)
            val controlPoint = controlPoints[pivot]

            when {
                controlPoint.time < time -> l = pivot + 1
                controlPoint.time > time -> r = pivot - 1
                else -> return pivot
            }
        }

        return l
    }
}
