package com.rian.difficultycalculator.utils;

/**
 * A utility for converting osu!standard hit windows and overall difficulty to some hit results and vice versa.
 */
public final class HitWindowConverter {
    private HitWindowConverter() {
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates the overall difficulty value of a great hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    public static float hitWindow300ToOD(double value) {
        return (float) (80 - value) / 6;
    }

    /**
     * Calculates the overall difficulty value of an ok hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    public static float hitWindow100ToOD(double value) {
        return (float) (140 - value) / 8;
    }

    /**
     * Calculates the overall difficulty value of a meh hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    public static float hitWindow50ToOD(double value) {
        return (float) (200 - value) / 10;
    }

    /**
     * Calculates the hit window for 300 (great) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow300(float od) {
        return 80 - 6 * od;
    }

    /**
     * Calculates the hit window for 100 (ok) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow100(float od) {
        return 140 - 8 * od;
    }

    /**
     * Calculates the hit window for 50 (meh) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    public static double odToHitWindow50(float od) {
        return 200 - 10 * od;
    }
}
