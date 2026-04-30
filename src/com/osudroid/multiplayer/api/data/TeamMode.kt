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
    TeamVersus;


    companion object {
        /**
         * Returns the [TeamMode] for [ordinal], or `null` if the ordinal is not recognised (EH-1).
         */
        operator fun get(ordinal: Int): TeamMode? = entries.getOrNull(ordinal)
    }
}