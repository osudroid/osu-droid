@file:JvmName("AngleConversions")
package com.rian.osu.math

/**
 * Converts this [Double] from degrees to radians.
 */
fun Double.toRadians() = this * Math.PI / 180

/**
 * Converts this [Float] from degrees to radians.
 */
fun Float.toRadians() = this * Math.PI.toFloat() / 180

/**
 * Converts this [Long] from degrees to radians.
 */
fun Long.toRadians() = this.toDouble().toRadians()

/**
 * Converts this [Int] from degrees to radians.
 */
fun Int.toRadians() = this.toFloat().toRadians()

/**
 * Converts this [Double] from radians to degrees.
 */
fun Double.toDegrees() = this * 180 / Math.PI

/**
 * Converts this [Float] from radians to degrees.
 */
fun Float.toDegrees() = this * 180 / Math.PI.toFloat()

/**
 * Converts this [Long] from radians to degrees.
 */
fun Long.toDegrees() = this.toDouble().toDegrees()

/**
 * Converts this [Int] from radians to degrees.
 */
fun Int.toDegrees() = this.toFloat().toDegrees()