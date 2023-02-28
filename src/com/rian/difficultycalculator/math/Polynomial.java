package com.rian.difficultycalculator.math;

/**
 * A single-variable polynomial with real-valued coefficients and non-negative exponents.
 */
public final class Polynomial {
    private Polynomial() {
        throw new UnsupportedOperationException();
    }

    /**
     * Evaluates a polynomial at point z.
     *
     * Coefficients are ordered ascending by power with power k at index k.
     * For example, coefficients <code>[3, -1, 2]</code> represent <code>y = 2x^2 - x + 3</code>.
     *
     * @param z The location where to evaluate the polynomial at.
     * @param coefficients The coefficients of the polynomial, coefficient for power k at index k.
     * @return The polynomial at z.
     */
    public static double evaluate(double z, double[] coefficients) {
        // Zero polynomials need explicit handling, otherwise we
        // will attempt to peek coefficients at negative indices.
        if (coefficients.length == 0) {
            return 0;
        }

        double sum = coefficients[coefficients.length - 1];

        for (int i = coefficients.length - 2; i >= 0; --i) {
            sum *= z;
            sum += coefficients[i];
        }

        return sum;
    }
}
