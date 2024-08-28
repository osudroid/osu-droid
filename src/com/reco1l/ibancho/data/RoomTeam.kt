package com.reco1l.ibancho.data

/**
 * Represents the team of a room player.
 */
enum class RoomTeam {

    /**
     * The red team.
     */
    RED,

    /**
     * The blue team.
     */
    BLUE;


    override fun toString() = when (this) {
        RED -> "Red Team"
        BLUE -> "Blue Team"
    }


    companion object {
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}