package com.reco1l.ibancho

import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomBeatmap
import com.reco1l.ibancho.data.RoomGameplaySettings
import com.reco1l.ibancho.data.RoomMods
import com.reco1l.ibancho.data.TeamMode
import com.reco1l.ibancho.data.WinCondition
import org.json.JSONArray

interface IRoomEventListener {

    // Connection related events

    /**
     * Emitted to a client when it connects to the room socket for the first time. Gives the following room info
     * structure as a parameter.
     *
     */
    fun onRoomConnect(newRoom: Room)

    /**
     * Called when the player disconnects from room socket, keep in mind this is also called when the user manually
     * disconnects.
     */
    fun onRoomDisconnect(reason: String?, byUser: Boolean)

    /**
     * Called when the connection fails.
     */
    fun onRoomConnectFail(error: String?)

    /**
     * Emitted when the server encounters an error in processing info (maybe this can be displayed in a toast message).
     * Gives a string as a parameter.
     */
    fun onServerError(error: String)

    // Settings related events

    /**
     * Emit when the host of the room was changed.
     *
     * @param uid The new room host UID.
     */
    fun onRoomHostChange(uid: Long)

    /**
     * Emit when the host changes mods in a non-free mods setting or speed-changing mods in free mods setting.
     */
    fun onRoomModsChange(mods: RoomMods)

    /**
     * Emit when the host changes a gameplay setting.
     */
    fun onRoomGameplaySettingsChange(settings: RoomGameplaySettings)

    /**
     * Emit when the host changes the team mode setting.
     */
    fun onRoomTeamModeChange(mode: TeamMode)

    /**
     * Emit when the host changes the win condition.
     */
    fun onRoomWinConditionChange(winCondition: WinCondition)

    /**
     * Emit when the host changes the name of the room.
     */
    fun onRoomNameChange(name: String)

    /**
     * Emit when the host changes the beatmap.
     */
    fun onRoomBeatmapChange(beatmap: RoomBeatmap?)

    // Match related events

    /**
     * Emit when the host initiates gameplay.
     */
    fun onRoomMatchPlay()

    /**
     * Emitted when all players have finished loading the beatmap after all clients receive the playBeatmap event.
     * Gameplay should only be started after receiving this event.
     */
    fun onRoomMatchStart()

    /**
     * Emitted when all players requested to skip a portion of the beatmap via the skip button.
     * The skip operation should only be performed after receiving this event.
     */
    fun onRoomMatchSkip()


    // Chat related events

    /**
     * Emit when the player sends a chat message.
     */
    fun onRoomChatMessage(uid: Long?, message: String)


    // Score related events

    /**
     * Emit when the player sends a live score data for real-time leaderboard.
     */
    fun onRoomLiveLeaderboard(leaderboard: JSONArray)

    /**
     * Emitted when all players have submitted their score.
     */
    fun onRoomFinalLeaderboard(leaderboard: JSONArray)
}