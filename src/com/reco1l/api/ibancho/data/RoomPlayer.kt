package com.reco1l.api.ibancho.data

data class RoomPlayer
(
        /**
         * The user ID.
         */
        val id: Long,

        /**
         * The username.
         */
        val name: String,

        /**
         * The player status.
         */
        var status: PlayerStatus,

        /**
         * The player team if the mode is set to team versus.
         */
        var team: RoomTeam?,

        /**
         * The player mods.
         */
        var mods: String?
)
{
        /**
         * Locally used to indicate the player is muted.
         */
        var isMuted = false
}
