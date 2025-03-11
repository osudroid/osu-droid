@file:JvmName("AngleConversions")
package com.rian.osu.math

/**
 * Converts a value from degrees to radians.
 */
fun Double.toRadians() = this * Math.PI / 180

/**
 * Converts a value from radians to degrees.
 */
fun Float.toRadians() = this * Math.PI.toFloat() / 180

/**
 * Converts a value from degrees to radians.
 */
fun Long.toRadians() = this.toDouble().toRadians()

/**
 * Converts a value from degrees to radians.
 */
fun Int.toRadians() = this.toFloat().toRadians()

/**
 * Converts a value from radians to degrees.
 */
fun Double.toDegrees() = this * 180 / Math.PI

/**
 * Converts a value from degrees to radians.
 */
fun Float.toDegrees() = this * 180 / Math.PI.toFloat()

/**
 * Converts a value from degrees to radians.
 */
fun Long.toDegrees() = this.toDouble().toDegrees()

/**
 * Converts a value from degrees to radians.
 */
fun Int.toDegrees() = this.toFloat().toDegrees()