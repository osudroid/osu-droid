package com.rian.osu.beatmap.constants

/**
 * Represents available sample banks.
 */
enum class SampleBank(
    /**
     * The prefix of audio files representing this sample bank.
     */
    @JvmField
    val prefix: String
) {
    None(""),
    Normal("normal"),
    Soft("soft"),
    Drum("drum");

    companion object {
        /**
         * Converts an integer value to its sample bank counterpart.
         *
         * @param value The value to convert.
         * @return The sample bank counterpart of the given value.
         */
        @JvmStatic
        fun parse(value: Int) =
            when (value) {
                1 -> Normal
                2 -> Soft
                3 -> Drum
                else -> None
            }

        /**
         * Converts a string value to its sample bank counterpart.
         *
         * @param value The value to convert.
         * @return The sample bank counterpart of the given value.
         */
        @JvmStatic
        fun parse(value: String?) =
            when (value) {
                "Normal" -> Normal
                "Soft" -> Soft
                "Drum" -> Drum
                else -> None
            }
    }
}
