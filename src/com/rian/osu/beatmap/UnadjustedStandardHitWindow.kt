package com.rian.osu.beatmap

/**
 * Represents the hit window of osu!standard that does not apply flooring and half-millisecond adjustments.
 *
 * **This class is only used for difficulty calculation and will be removed later**.
 */
class UnadjustedStandardHitWindow@JvmOverloads constructor(
    /**
     * The overall difficulty of this [UnadjustedStandardHitWindow]. Defaults to 5.
     */
    overallDifficulty: Double? = 5.0
) : HitWindow(overallDifficulty) {
    /**
     * Creates a new [UnadjustedStandardHitWindow] with the specified overall difficulty.
     * The overall difficulty will be converted to a [Double].
     *
     * @param overallDifficulty The overall difficulty of this [UnadjustedStandardHitWindow]. Defaults to 5.
     */
    constructor(overallDifficulty: Float? = 5f) : this(overallDifficulty?.toDouble())

    override val greatWindow
        get() = 80 - 6 * overallDifficulty

    override val okWindow
        get() = 140 - 8 * overallDifficulty

    override val mehWindow
        get() = 200 - 10 * overallDifficulty

    companion object {
        /**
         * Calculates the overall difficulty value of a great hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @return The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow300ToOverallDifficulty(value: Double) = (80 - value) / 6

        /**
         * Calculates the overall difficulty value of an ok hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @returns The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow100ToOverallDifficulty(value: Double) = (140 - value) / 8

        /**
         * Calculates the overall difficulty value of a meh hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @returns The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow50ToOverallDifficulty(value: Double) = (200 - value) / 10
    }
}