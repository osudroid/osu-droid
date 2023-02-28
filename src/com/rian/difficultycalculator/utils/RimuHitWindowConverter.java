package com.rian.difficultycalculator.utils;

/**
 * A utility for converting rimu! hit windows or overall difficulty to some hit results and vice versa.
 */
public final class RimuHitWindowConverter {
    private RimuHitWindowConverter() {
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates the overall difficulty value of a great hit window without the Precise mod.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    public static double hitWindow300ToOD(double value) {
        return hitWindow300ToOD(value, false);
    }

    /**
     * Calculates the overall difficulty value of a great hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @param isPrecise Whether to calculate for Precise mod.
     * @return The overall difficulty value.
     */
    public static double hitWindow300ToOD(double value, boolean isPrecise) {
        if (isPrecise) {
            return 5 - (value - 55) / 6;
        } else {
            return 5 - (value - 75) / 5;
        }
    }

    /**
     * Calculates the overall difficulty value of a good hit window without the Precise mod.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    public static double hitWindow100ToOD(double value) {
        return hitWindow100ToOD(value, false);
    }

    /**
     * Calculates the overall difficulty value of a good hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @param isPrecise Whether to calculate for Precise mod.
     * @return The overall difficulty value.
     */
    public static double hitWindow100ToOD(double value, boolean isPrecise) {
        if (isPrecise) {
            return 5 - (value - 120) / 8;
        } else {
            return 5 - (value - 150) / 10;
        }
    }

    /**
     * Calculates the overall difficulty value of a meh hit window without the Precise mod.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    public static double hitWindow50ToOD(double value) {
        return hitWindow50ToOD(value, false);
    }

    /**
     * Calculates the overall difficulty value of a meh hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @param isPrecise Whether to calculate for Precise mod.
     * @return The overall difficulty value.
     */
    public static double hitWindow50ToOD(double value, boolean isPrecise) {
        if (isPrecise) {
            return 5 - (value - 180) / 10;
        } else {
            return 5 - (value - 250) / 10;
        }
    }

    /**
     * Calculates the hit window for 300 (great) hit result of an overall difficulty value
     * without the Precise mod.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow300(double od) {
        return odToHitWindow300(od, false);
    }

    /**
     * Calculates the hit window for 300 (great) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @param isPrecise Whether to calculate for Precise mod.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow300(double od, boolean isPrecise) {
        if (isPrecise) {
            return 55 + 6 * (5 - od);
        } else {
            return 75 + 5 * (5 - od);
        }
    }

    /**
     * Calculates the hit window for 100 (good) hit result of an overall difficulty value
     * without the Precise mod.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow100(double od) {
        return odToHitWindow100(od, false);
    }

    /**
     * Calculates the hit window for 100 (good) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @param isPrecise Whether to calculate for Precise mod.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow100(double od, boolean isPrecise) {
        if (isPrecise) {
            return 120 + 8 * (5 - od);
        } else {
            return 150 + 10 * (5 - od);
        }
    }

    /**
     * Calculates the hit window for 50 (meh) hit result of an overall difficulty value without the
     * Precise mod.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow50(double od) {
        return odToHitWindow50(od, false);
    }

    /**
     * Calculates the hit window for 50 (meh) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @param isPrecise Whether to calculate for Precise mod.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow50(double od, boolean isPrecise) {
        if (isPrecise) {
            return 180 + 10 * (5 - od);
        } else {
            return 250 + 10 * (5 - od);
        }
    }
}
