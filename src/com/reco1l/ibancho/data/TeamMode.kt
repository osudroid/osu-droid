package com.reco1l.ibancho.data

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
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}