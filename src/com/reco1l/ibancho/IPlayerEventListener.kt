package com.reco1l.ibancho

import com.reco1l.ibancho.data.PlayerStatus
import com.reco1l.ibancho.data.RoomPlayer
import com.reco1l.ibancho.data.RoomTeam
import com.reco1l.ibancho.data.RoomMods

interface IPlayerEventListener {

    /**
     * Emitted when a player joins the room. Gives the following structure as a parameter:
     */
    fun onPlayerJoin(player: RoomPlayer)

    /**
     * Emitted when a socket disconnects from the server.
     */
    fun onPlayerLeft(uid: Long)

    /**
     * Emit when the host kicks a player.
     *
     * @param uid The UID of the kicked player.
     */
    fun onPlayerKick(uid: Long)

    /**
     * Emit when the player changes player-specific mods in free mods setting.
     *
     * @param uid The player UID that changed mods.
     * @param mods The mods string.
     */
    fun onPlayerModsChange(uid: Long, mods: RoomMods)

    /**
     * Emit when the player changes their state.
     */
    fun onPlayerStatusChange(uid: Long, status: PlayerStatus)

    /**
     * Emit when the player changes their team in Team VS team mode.
     */
    fun onPlayerTeamChange(uid: Long, team: RoomTeam?)

}