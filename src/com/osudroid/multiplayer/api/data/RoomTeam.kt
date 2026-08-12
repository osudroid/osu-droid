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
         * Returns the [RoomTeam] for the given wire string, or `null` if not recognized.
         */
        fun fromWire(value: String): RoomTeam? = runCatching { enumValueOf<RoomTeam>(value) }.getOrNull()
    }
}
