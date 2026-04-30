package com.osudroid.multiplayer.api.data

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
        /**
         * Returns the [RoomTeam] for [ordinal], or `null` if the ordinal is not recognised (EH-1).
         */
        operator fun get(ordinal: Int): RoomTeam? = entries.getOrNull(ordinal)
    }
}