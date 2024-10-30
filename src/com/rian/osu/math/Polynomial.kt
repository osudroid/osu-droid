package com.rian.osu.math

/**
 * A single-variable polynomial with real-valued coefficients and non-negative exponents.
 *
 * This object shares the same implementation as [Math.NET Numerics](https://numerics.mathdotnet.com/).
 */
object Polynomial {
    /**
     * Evaluates a polynomial at point [z].
     *
     * Coefficients are ordered ascending by power with power `k` at index `k`.
     * For example, coefficients `[3, -1, 2]` represent `y = 2x^2 - x + 3`.
     *
     * @param z The location where to evaluate the polynomial at.
     * @param coefficients The coefficients of the polynomial, coefficient for power `k` at index `k`.
     * @returns The polynomial at [z].
     */
    fun evaluate(z: Double, coefficients: DoubleArray): Double {
        // Zero polynomials need explicit handling, otherwise we
        // will attempt to peek coefficients at negative indices.
        if (coefficients.isEmpty()) {
            return 0.0
        }

        var sum = coefficients[coefficients.size - 1]

        for (i in coefficients.size - 2 downTo 0) {
            sum *= z
            sum += coefficients[i]
        }

        return sum
    }
}