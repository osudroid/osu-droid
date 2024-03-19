package com.rian.osu.beatmap.constants

/**
 * Available sections in a `.osu` beatmap file.
 */
enum class BeatmapSection {
    General,
    Editor,
    Metadata,
    Difficulty,
    Events,
    TimingPoints,
    Colors,
    HitObjects;

    companion object {
        /**
         * Converts a string section value from a beatmap file to its enum counterpart.
         *
         * @param value The value to convert.
         * @return The enum representing the value.
         */
        @JvmStatic
        fun parse(value: String?) =
            when (value) {
                "General" -> General
                "Editor" -> Editor
                "Metadata" -> Metadata
                "Difficulty" -> Difficulty
                "Events" -> Events
                "TimingPoints" -> TimingPoints
                "Colours" -> Colors
                "HitObjects" -> HitObjects
                else -> null
            }
    }
}
