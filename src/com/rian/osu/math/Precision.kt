package com.rian.osu.math

import kotlin.math.abs

/**
 * Precision utilities.
 */
object Precision {
    /**
     * The default epsilon for all [Float] values.
     */
    const val FLOAT_EPSILON = 1e-3f

    /**
     * The default epsilon for all [Double] values.
     */
    const val DOUBLE_EPSILON = 1e-7

    /**
     * Checks if a [Float] is definitely greater than another [Float] with a given tolerance.
     *
     * @param value1 The first [Float].
     * @param value2 The second [Float].
     * @param acceptableDifference The acceptable difference. Defaults to [FLOAT_EPSILON].
     * @return Whether [value1] is definitely greater than [value2].
     */
    @JvmStatic
    @JvmOverloads
    fun definitelyBigger(value1: Float, value2: Float, acceptableDifference: Float = FLOAT_EPSILON) =
        value1 - acceptableDifference > value2

    /**
     * Checks if a [Double] is definitely greater than another [Double] with a given tolerance.
     *
     * @param value1 The first [Double].
     * @param value2 The second [Double].
     * @param acceptableDifference The acceptable difference. Defaults to [DOUBLE_EPSILON].
     * @return Whether [value1] is definitely greater than [value2].
     */
    @JvmStatic
    @JvmOverloads
    fun definitelyBigger(value1: Double, value2: Double, acceptableDifference: Double = DOUBLE_EPSILON) =
        value1 - acceptableDifference > value2

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
