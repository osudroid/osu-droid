package com.rian.osu.math

import kotlin.math.pow

/**
 * Holds interpolation methods for numbers.
 */
object Interpolation {
    /**
     * Performs a linear interpolation.
     *
     * @param start The starting point of the interpolation.
     * @param end The final point of the interpolation.
     * @param amount The interpolation multiplier.
     * @return The interpolated value.
     */
    @JvmStatic
    fun linear(start: Double, end: Double, amount: Double) = start + (end - start) * amount

    /**
     * Performs a linear interpolation.
     *
     * @param start The starting point of the interpolation.
     * @param end The final point of the interpolation.
     * @param amount The interpolation multiplier.
     * @return The interpolated value.
     */
    @JvmStatic
    fun linear(start: Float, end: Float, amount: Float) = start + (end - start) * amount

    /**
     * Performs a linear interpolation.
     *
     * @param start The starting point of the interpolation.
     * @param end The final point of the interpolation.
     * @param amount The interpolation multiplier.
     * @return The interpolated value.
     */
    @JvmStatic
    fun linear(start: Vector2, end: Vector2, amount: Float) = Vector2(
        linear(start.x, end.x, amount),
        linear(start.y, end.y, amount)
    )

    /**
     * Interpolates between [start] and [end] using a given [base] and [exponent].
     *
     * @param start The starting point of the interpolation.
     * @param end The final point of the interpolation.
     * @param base The base of the exponential. The valid range is `[0, 1]`, where smaller values mean that [end] is
     * achieved more quickly, and values closer to 1 results in slow convergence to [end].
     * @param exponent The exponent of the exponential. An exponent of 0 results in [start], whereas larger
     * exponents make the result converge to [end].
     * @return The interpolated value.
     */
    @JvmStatic
    fun damp(start: Float, end: Float, base: Float, exponent: Float): Float {
        if (base < 0 || base > 1) {
            throw IllegalArgumentException("Base must be in the range [0, 1].")
        }

        return linear(start, end, 1 - base.pow(exponent))
    }

    /**
     * Interpolates [current] towards [target] based on [elapsedTime]. If [current] is updated every frame using this
     * function, the result is approximately frame-rate independent.
     *
     * Because floating-point errors can accumulate over a long time, this function should not be used for things
     * requiring accurate values.
     *
     * @param current The current value.
     * @param target The target value.
     * @param halfTime The time taken for the value to reach the middle value of [current] and [target].
     * @param elapsedTime The elapsed time of the current frame.
     * @return The interpolated value.
     */
    @JvmStatic
    fun dampContinuously(current: Float, target: Float, halfTime: Float, elapsedTime: Float) =
        damp(current, target, 0.5f, elapsedTime / halfTime)
}