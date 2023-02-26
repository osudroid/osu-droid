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
        return almostEqualsNumber((double) value1, (double) value2, (double) acceptableDifference);
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

    /**
     * Checks whether two real numbers are almost equal.
     *
     * @param a The first number.
     * @param b The second number.
     * @return Whether the two values differ by no more than 10 * 2^(-52).
     */
    public static boolean almostEqualRelative(double a, double b) {
        return almostEqualNormRelative(a, b, a - b, 10 * Math.pow(2, -53));
    }

    /**
     * Compares two numbers and determines if they are equal within the specified maximum error.
     *
     * @param a The norm of the first value (can be negative).
     * @param b The norm of the second value (can be negative).
     * @param diff The norm of the difference of the two values (can be negative).
     * @param maximumError The accuracy required for being almost equal.
     * @returns Whether both numbers are almost equal up to the specified maximum error.
     */
    public static boolean almostEqualNormRelative(double a, double b, double diff, double maximumError) {
        // If A or B are infinity (positive or negative) then
        // only return true if they are exactly equal to each other -
        // that is, if they are both infinities of the same sign.
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return a == b;
        }

        // If A or B are a NAN, return false. NANs are equal to nothing,
        // not even themselves.
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }

        // If one is almost zero, fall back to absolute equality.
        double doublePrecision = Math.pow(2, -53);
        if (Math.abs(a) < doublePrecision || Math.abs(b) < doublePrecision) {
            return Math.abs(diff) < maximumError;
        }

        if ((a == 0 && Math.abs(b) < maximumError) || (b == 0 && Math.abs(a) < maximumError)) {
            return true;
        }

        return (Math.abs(diff) < maximumError * Math.max(Math.abs(a), Math.abs(b)));
    }
}
