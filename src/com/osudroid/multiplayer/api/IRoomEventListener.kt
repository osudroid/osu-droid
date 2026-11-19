package com.osudroid.multiplayer.api

import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.session.MultiplayerRecoveryPayload
import org.json.JSONArray

interface IRoomEventListener {

    // Connection related events

    /**
     * Emitted to a client when it connects to the room socket, including when it reconnects after a drop.
     *
     * @param newRoom The room info the server sent.
     * @param recovery What the server reported about the match in progress and the chat the client may have
     * missed due to reconnection.
     */
    fun onRoomConnect(newRoom: Room, recovery: MultiplayerRecoveryPayload)

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
     * Called when a [MultiplayerClientEvent] the client sent was not delivered to the server, so the action behind it
     * did not happen.
     *
     * @param event The [MultiplayerClientEvent] that was not delivered.
     * @param whileConnected Whether the socket was connected and the server did not acknowledge the event in time, as
     * opposed to the socket being disconnected when the [MultiplayerClientEvent] was sent or while it was waiting for
     * an acknowledgement.
     */
    fun onEventNotDelivered(event: MultiplayerClientEvent, whileConnected: Boolean)

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
     * Emit when the host changes the maximum amount of players allowed in the room.
     */
    fun onRoomMaxPlayersChange(maxPlayers: Int)

    /**
     * Emit when the host changes the beatmap.
     */
    fun onRoomBeatmapChange(beatmap: RoomBeatmap?)

    // Match related events

    /**
     * Emit when the host initiates gameplay.
     *
     * @param roundId The ID of the round that is starting.
     */
    fun onRoomMatchPlay(roundId: Int)

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
     *
     * @param leaderboard The final leaderboard.
     * @param roundId The ID of the round the leaderboard belongs to.
     */
    fun onRoomFinalLeaderboard(leaderboard: JSONArray, roundId: Int)
}