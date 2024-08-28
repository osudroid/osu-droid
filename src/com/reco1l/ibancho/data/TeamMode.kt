package com.reco1l.ibancho.data

/**
 * Represents the team mode of a room.
 */
enum class TeamMode {

    /**
     * Players are playing individually head-to-head.
     */
    HEAD_TO_HEAD,

    /**
     * Players are playing in teams.
     */
    TEAM_VS_TEAM;


    companion object {
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}