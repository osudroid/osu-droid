package com.rian.osu.beatmap

/**
 * Represents the osu!standard hit window.
 */
class StandardHitWindow @JvmOverloads constructor(
    /**
     * The overall difficulty of this [StandardHitWindow]. Defaults to 5.
     */
    overallDifficulty: Float = 5f
) : HitWindow(overallDifficulty) {
    constructor(overallDifficulty: Float?) : this(overallDifficulty ?: 5f)

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
        fun hitWindow300ToOverallDifficulty(value: Float) = (80 - value) / 6

        /**
         * Calculates the overall difficulty value of an ok hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @returns The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow100ToOverallDifficulty(value: Float) = (140 - value) / 8

        /**
         * Calculates the overall difficulty value of a meh hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @returns The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow50ToOverallDifficulty(value: Float) = (200 - value) / 10
    }
}