package com.reco1l.ibancho.data

/**
 * Represents the team of a room player.
 */
enum class RoomTeam {

    /**
     * The red team.
     */
    Red,

    /**
     * The blue team.
     */
    Blue;


    override fun toString() = when (this) {
        Red -> "Red Team"
        Blue -> "Blue Team"
    }


    companion object {
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}