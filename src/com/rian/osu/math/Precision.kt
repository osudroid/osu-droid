package com.rian.osu.math

import kotlin.math.abs

/**
 * Precision utilities.
 */
object Precision {
    /**
     * The default epsilon for all [Float] values.
     */
    private const val FLOAT_EPSILON = 1e-3f

    /**
     * The default epsilon for all [Double] values.
     */
    private const val DOUBLE_EPSILON = 1e-7

    /**
     * Checks if two numbers are equal with a given tolerance.
     *
     * @param value1 The first number.
     * @param value2 The second number.
     * @param acceptableDifference The acceptable difference as threshold.
     */
    @JvmStatic
    @JvmOverloads
    fun almostEquals(value1: Float, value2: Float, acceptableDifference: Float = FLOAT_EPSILON) =
        abs(value1 - value2) <= acceptableDifference

    /**
     * Checks if two numbers are equal with a given tolerance.
     *
     * @param value1 The first number.
     * @param value2 The second number.
     * @param acceptableDifference The acceptable difference as threshold.
     */
    @JvmStatic
    @JvmOverloads
    fun almostEquals(value1: Double, value2: Double, acceptableDifference: Double = DOUBLE_EPSILON) =
        abs(value1 - value2) <= acceptableDifference
}
