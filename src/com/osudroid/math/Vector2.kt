package com.osudroid.math

import android.graphics.PointF
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Represents a two-dimensional vector.
 */
data class Vector2(
    /**
     * The X position of the vector.
     */
    @JvmField
    var x: Float,

    /**
     * The Y position of the vector.
     */
    @JvmField
    var y: Float
) {
    constructor(value: Int): this(value, value)
    constructor(x: Int, y: Int): this(x.toFloat(), y.toFloat())
    constructor(value: Float) : this(value, value)
    constructor(pointF: PointF) : this(pointF.x, pointF.y)

    /**
     * The length of this vector.
     */
    val length: Float
        get() = hypot(x, y)

    /**
     * The square of this vector's length (magnitude).
     *
     * This getter eliminates the costly square root operation required by the
     * [length] property. This makes it more suitable for comparisons.
     */
    val lengthSquared: Float
        get() = x * x + y * y

    /**
     * Performs a dot multiplication with another vector.
     *
     * @param vec The other vector.
     * @return The dot product of both vectors.
     */
    fun dot(vec: Vector2) = x * vec.x + y * vec.y

    /**
     * Gets the distance between this vector and another vector.
     *
     * @param vec The other vector.
     * @return The distance between this vector and the other vector.
     */
    fun getDistance(vec: Vector2) = hypot(vec.x - x, vec.y - y)

    /**
     * Gets the square of distance between this vector and another vector.
     *
     * This avoids square root calculation and is suitable for distance comparisons.
     *
     * @param vec The other vector.
     * @return The square of distance between this vector and the other vector.
     */
    fun getDistanceSquared(vec: Vector2): Float {
        val dx = vec.x - x
        val dy = vec.y - y

        return dx * dx + dy * dy
    }

    /**
     * Gets the square of distance between this vector and a point.
     *
     * @param x The X coordinate of the point.
     * @param y The Y coordinate of the point.
     * @return The square of distance between this vector and the point.
     */
    fun getDistanceSquared(x: Float, y: Float): Float {
        val dx = x - this.x
        val dy = y - this.y

        return dx * dx + dy * dy
    }

    /**
     * Normalizes the vector.
     */
    fun normalize() {
        val lengthSquared = lengthSquared

        if (lengthSquared == 0f) {
            return
        }

        val inverseLength = 1f / sqrt(lengthSquared)

        x *= inverseLength
        y *= inverseLength
    }

    /**
     * Converts this vector to a [PointF].
     *
     * @return The converted [PointF].
     */
    fun toPointF() = PointF(x, y)

    /**
     * Multiplies this vector with another vector.
     *
     * @param vec The other vector.
     * @return The multiplied vector.
     */
    operator fun times(vec: Vector2) = Vector2(x * vec.x, y * vec.y)

    /**
     * Scales this vector.
     *
     * @param scaleFactor The factor to scale the vector by.
     * @return The scaled vector.
     */
    operator fun times(scaleFactor: Int) = times(scaleFactor.toFloat())

    /**
     * Scales this vector.
     *
     * @param scaleFactor The factor to scale the vector by.
     * @return The scaled vector.
     */
    operator fun times(scaleFactor: Float) = Vector2(x * scaleFactor, y * scaleFactor)

    /**
     * Scales this vector.
     *
     * @param scaleFactor The factor to scale the vector by.
     * @return The scaled vector.
     */
    operator fun times(scaleFactor: Double) = times(scaleFactor.toFloat())

    /**
     * Divides this vector with a scalar.
     *
     * Attempting to divide by 0 will throw an error.
     *
     * @param divideFactor The factor to divide the vector by.
     * @return The divided vector.
     */
    operator fun div(divideFactor: Int) = div(divideFactor.toFloat())

    /**
     * Divides this vector with a scalar.
     *
     * Attempting to divide by 0 will throw an error.
     *
     * @param divideFactor The factor to divide the vector by.
     * @return The divided vector.
     */
    operator fun div(divideFactor: Float) =
        if (divideFactor == 0f) throw ArithmeticException("Division by 0")
        else Vector2(x / divideFactor, y / divideFactor)

    /**
     * Divides this vector with a scalar.
     *
     * Attempting to divide by 0 will throw an error.
     *
     * @param divideFactor The factor to divide the vector by.
     * @return The divided vector.
     */
    operator fun div(divideFactor: Double) = div(divideFactor.toFloat())

    /**
     * Adds each component of this vector with a scalar.
     *
     * @param n The scalar to add.
     * @return The added vector.
     */
    operator fun plus(n: Float) = Vector2(x + n, y + n)

    /**
     * Adds this vector with another vector.
     *
     * @param vec The other vector.
     * @return The added vector.
     */
    operator fun plus(vec: Vector2) = Vector2(x + vec.x, y + vec.y)

    /**
     * Adds each component of this vector with a scalar.
     *
     * Keep in mind that this operation will mutate this vector instance.
     *
     * @param n The scalar to add.
     */
    operator fun plusAssign(n: Float) {
        x += n
        y += n
    }

    /**
     * Adds this vector with another vector.
     *
     * Keep in mind that this operation will mutate this vector instance.
     *
     * @param vec The other vector.
     */
    operator fun plusAssign(vec: Vector2) {
        x += vec.x
        y += vec.y
    }

    /**
     * Subtracts each component of this vector with a scalar.
     *
     * @param n The scalar to subtract.
     */
    operator fun minus(n: Float) = Vector2(x - n, y - n)

    /**
     * Subtracts this vector with another vector.
     *
     * @param vec The other vector.
     * @return The subtracted vector.
     */
    operator fun minus(vec: Vector2) = Vector2(x - vec.x, y - vec.y)

    /**
     * Subtracts each component of this vector with a scalar.
     *
     * Keep in mind that this operation will mutate this vector instance.
     *
     * @param n The scalar to subtract.
     */
    operator fun minusAssign(n: Float) {
        x -= n
        y -= n
    }

    /**
     * Subtracts this vector with another vector.
     *
     * Keep in mind that this operation will mutate this vector instance.
     *
     * @param vec The other vector.
     */
    operator fun minusAssign(vec: Vector2) {
        x -= vec.x
        y -= vec.y
    }

    /**
     * Negates this vector.
     *
     * @return The negated vector.
     */
    operator fun unaryMinus() = Vector2(-x, -y)

    companion object {
        /**
         * Gets the square of distance between two points.
         */
        @JvmStatic
        fun distanceSquared(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x2 - x1
            val dy = y2 - y1

            return dx * dx + dy * dy
        }
    }
}
