package com.rian.difficultycalculator.math;

/**
 * Holds interpolation methods for numbers.
 */
public final class Interpolation {
    private Interpolation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Performs a linear interpolation.
     *
     * @param start The starting point of the interpolation.
     * @param end The final point of the interpolation.
     * @param amount The interpolation multiplier.
     * @return The interpolated value.
     */
    public static double linear(double start, double end, double amount) {
        return start + (end - start) * amount;
    }
}
