package com.rian.difficultycalculator.math;

/**
 * Precision utilities.
 */
public final class Precision {
    /**
     * The default epsilon for all <code>float</code> values.
     */
    public static final float FLOAT_EPSILON = 1e-3f;

    /**
     * The default epsilon for all <code>double</code> values.
     */
    public static final double DOUBLE_EPSILON = 1e-7;

    /**
     * Checks if two numbers are equal with a given tolerance.
     *
     * @param value1 The first number.
     * @param value2 The second number.
     */
    public static boolean almostEqualsNumber(float value1, float value2) {
        return almostEqualsNumber(value1, value2, FLOAT_EPSILON);
    }

    /**
     * Checks if two numbers are equal with a given tolerance.
     *
     * @param value1 The first number.
     * @param value2 The second number.
     * @param acceptableDifference The acceptable difference as threshold.
     */
    public static boolean almostEqualsNumber(float value1, float value2, float acceptableDifference) {
        return almostEqualsNumber(value1, value2, (double) acceptableDifference);
    }

    /**
     * Checks if two numbers are equal with a given tolerance.
     *
     * @param value1 The first number.
     * @param value2 The second number.
     */
    public static boolean almostEqualsNumber(double value1, double value2) {
        return almostEqualsNumber(value1, value2, DOUBLE_EPSILON);
    }

    /**
     * Checks if two numbers are equal with a given tolerance.
     *
     * @param value1 The first number.
     * @param value2 The second number.
     */
    public static boolean almostEqualsNumber(double value1, double value2, double acceptableDifference) {
        return Math.abs(value1 - value2) <= acceptableDifference;
    }
}
