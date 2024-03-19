package com.rian.osu.math

import android.graphics.PointF
import kotlin.math.hypot

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
     * Normalizes the vector.
     */
    fun normalize() {
        val length = length

        x /= length
        y /= length
    }

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
}
