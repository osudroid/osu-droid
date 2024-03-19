@file:JvmName("Vector2Utils")

package com.rian.osu.math

/**
 * Multiplies this integer to a [Vector2].
 *
 * @param vec The [Vector2] to multiply to.
 * @return A [Vector2] scaled with this integer.
 */
operator fun Int.times(vec: Vector2) = vec * this

/**
 * Multiplies this float to a [Vector2].
 *
 * @param vec The [Vector2] to multiply to.
 * @return A [Vector2] scaled with this float.
 */
operator fun Float.times(vec: Vector2) = vec * this

/**
 * Multiplies this double to a [Vector2].
 *
 * @param vec The [Vector2] to multiply to.
 * @return A [Vector2] scaled with this double.
 */
operator fun Double.times(vec: Vector2) = vec * this

/**
 * Creates a new [Vector2] with its X and Y position set to this float.
 */
fun Float.toVector2() = Vector2(this, this)

/**
 * Creates a new [Vector2] with its X and Y position set to the first and second float of this pair respectively.
 */
fun Pair<Float, Float>.toVector2() = Vector2(first, second)