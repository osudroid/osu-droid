package com.rian.osu.beatmap

import com.rian.osu.mods.ModPrecise

/**
 * Represents an osu!droid hit window with the [ModPrecise] mod.
 */
class PreciseDroidHitWindow(
    /**
     * The overall difficulty of this [PreciseDroidHitWindow].
     */
    overallDifficulty: Float
) : HitWindow(overallDifficulty) {
    override val greatWindow = 55 + 6 * (5 - overallDifficulty)
    override val okWindow = 120 + 8 * (5 - overallDifficulty)
    override val mehWindow = 180 + 10 * (5 - overallDifficulty)

    companion object {
        /**
         * Calculates the overall difficulty value of a great hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @return The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow300ToOverallDifficulty(value: Float) = 5 - (value - 55) / 6

        /**
         * Calculates the overall difficulty value of an ok hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @returns The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow100ToOverallDifficulty(value: Float) = 5 - (value - 120) / 8

        /**
         * Calculates the overall difficulty value of a meh hit window.
         *
         * @param value The value of the hit window in milliseconds.
         * @returns The overall difficulty value.
         */
        @JvmStatic
        fun hitWindow50ToOverallDifficulty(value: Float) = 5 - (value - 180) / 10
    }
}