package com.osudroid.multiplayer.api.data

/**
 * Represents the team mode of a room.
 */
enum class TeamMode {

    /**
     * Players are playing individually head-to-head.
     */
    HeadToHead,

    /**
     * Players are playing in teams.
     */
    TeamVS;


    companion object {
        /**
         * Returns the [TeamMode] for the given wire string, or `null` if not recognized.
         */
        fun fromWire(value: String): TeamMode? = runCatching { enumValueOf<TeamMode>(value) }.getOrNull()
    }
}
