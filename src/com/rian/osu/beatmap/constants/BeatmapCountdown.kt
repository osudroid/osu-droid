package com.rian.osu.beatmap.constants

/**
 * Represents the speed of the countdown before the first hit object.
 */
enum class BeatmapCountdown(
    /**
     * The speed at which the beatmap countdown should be played.
     */
    @JvmField
    val speed: Float
) {
    NoCountdown(0f),
    Normal(1f),
    Half(0.5f),
    Twice(2f);

    companion object {
        /**
         * Converts a string data from a beatmap file to its enum counterpart.
         *
         * @param data The data to convert.
         * @return The enum representing the data.
         */
        @JvmStatic
        fun parse(data: String?) =
            when (data) {
                "0" -> NoCountdown
                "2" -> Half
                "3" -> Twice
                else -> Normal
            }
        }
}
