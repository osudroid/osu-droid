package com.rian.osu.utils

/**
 * A utility for converting osu!standard hit windows and overall difficulty to some hit results and vice versa.
 */
object HitWindowConverter {
    /**
     * Calculates the overall difficulty value of a great hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    @JvmStatic
    fun hitWindow300ToOD(value: Double) = (80 - value).toFloat() / 6

    /**
     * Calculates the overall difficulty value of an ok hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    @JvmStatic
    fun hitWindow100ToOD(value: Double) = (140 - value).toFloat() / 8

    /**
     * Calculates the overall difficulty value of a meh hit window.
     *
     * @param value The value of the hit window, in milliseconds.
     * @return The overall difficulty value.
     */
    @JvmStatic
    fun hitWindow50ToOD(value: Double) = (200 - value).toFloat() / 10

    /**
     * Calculates the hit window for 300 (great) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    @JvmStatic
    fun odToHitWindow300(od: Float) = (80 - 6 * od).toDouble()

    /**
     * Calculates the hit window for 100 (ok) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    @JvmStatic
    fun odToHitWindow100(od: Float) = (140 - 8 * od).toDouble()

    /**
     * Calculates the hit window for 50 (meh) hit result of an overall difficulty value.
     *
     * @param od The overall difficulty value.
     * @return The hit window in milliseconds.
     */
    @JvmStatic
    fun odToHitWindow50(od: Float) = (200 - 10 * od).toDouble()
}