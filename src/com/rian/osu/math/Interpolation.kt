package com.rian.osu.math

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
}