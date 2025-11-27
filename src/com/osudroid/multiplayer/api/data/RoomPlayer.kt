package com.osudroid.multiplayer.api.data

import com.osudroid.multiplayer.*

data class RoomPlayer(

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
    var mods: RoomMods

) {

    /**
     * Whether this player is the host of the room.
     */
    val isHost
        get() = Multiplayer.room?.host == id


    /**
     * Locally used to indicate the player is muted.
     */
    var isMuted = false


    override fun equals(other: Any?): Boolean {
        return other === this || other is RoomPlayer && other.id == id
    }
}
