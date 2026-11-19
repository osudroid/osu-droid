package com.osudroid.debug

import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.MultiplayerClientEvent
import com.osudroid.multiplayer.api.MultiplayerServerEvent
import io.socket.client.*
import io.socket.emitter.*
import org.json.*
import ru.nsu.ccfit.zuev.osu.Config

/**
 * Creates a mock multiplayer room.
 */
fun MockRoom() = Room(
    id = 1,
    name = "Test Room",
    isLocked = false,
    maxPlayers = 8,
    mods = RoomMods(),
    gameplaySettings = RoomGameplaySettings(isFreeMod = true, isRemoveSliderLock = false),
    teamMode = TeamMode.HeadToHead,
    winCondition = WinCondition.ScoreV1,
    playerCount = 0,
    playerNames = "",
    status = RoomStatus.Idle
)

/**
 * Creates a mock multiplayer socket.
 */
class MockSocket(private val uid: Long) : Socket(null, null, null) {
    private var isConnected = false

    // Continue from the session so that a mock reconnection does not restart numbering,
    // which the session would drop as duplicates.
    private var currentRoundId = Multiplayer.session.activeRoundId ?: 0
    private var currentChatId = Multiplayer.session.lastChatId ?: 0

    override fun connected() = isConnected

    override fun emit(event: String?, vararg args: Any?): Emitter {
        dispatch(event, arrayOf(*args))

        return this
    }

    override fun emit(event: String?, args: Array<Any?>?, ack: Ack?): Emitter {
        dispatch(event, args ?: arrayOf())

        // The server acknowledges every event it accepts.
        ack?.call(JSONObject().put("ok", true))

        return this
    }

    /**
     * Emulates the server's response to a [MultiplayerClientEvent].
     */
    private fun dispatch(event: String?, args: Array<Any?>) {

        val responseEvent = when (event) {

            // In these events the client expects a JSONArray as a response.
            MultiplayerClientEvent.ScoreSubmission.eventName ->
                MultiplayerServerEvent.AllPlayersScoreSubmitted.eventName to arrayOf(JSONArray().apply { put(args[0]) }, currentRoundId)

            MultiplayerClientEvent.LiveScoreData.eventName -> MultiplayerServerEvent.LiveScoreData.eventName to arrayOf(JSONArray().apply {
                // The username is expected to be added by the server.
                put((args[0] as JSONObject).put("username", Config.getOnlineUsername()))
            })

            // The server starts a new round.
            MultiplayerClientEvent.PlayBeatmap.eventName -> MultiplayerServerEvent.PlayBeatmap.eventName to arrayOf(++currentRoundId)

            // Simulating the server response when all players emit these events.
            MultiplayerClientEvent.BeatmapLoadComplete.eventName -> MultiplayerServerEvent.AllPlayersBeatmapLoadComplete.eventName to arrayOf()
            MultiplayerClientEvent.SkipRequested.eventName -> MultiplayerServerEvent.AllPlayersSkipRequested.eventName to arrayOf()

            // The server gives every public chat message an increasing ID.
            MultiplayerClientEvent.ChatMessage.eventName ->
                MultiplayerServerEvent.ChatMessage.eventName to arrayOf(uid.toString(), args[0], ++currentChatId)

            // In these events the client expects to receive the uid of the player as the first argument.
            MultiplayerClientEvent.TeamChanged.eventName -> MultiplayerServerEvent.TeamChanged.eventName to arrayOf(uid.toString(), args[0])
            MultiplayerClientEvent.PlayerModsChanged.eventName -> MultiplayerServerEvent.PlayerModsChanged.eventName to arrayOf(uid.toString(), args[0])
            MultiplayerClientEvent.PlayerStatusChanged.eventName -> MultiplayerServerEvent.PlayerStatusChanged.eventName to arrayOf(uid.toString(), args[0])

            // In these events, the client expects to receive an integer as the first argument.
            MultiplayerClientEvent.MaxPlayersChanged.eventName -> MultiplayerServerEvent.MaxPlayersChanged.eventName to arrayOf(args[0].toString())

            // These events require special handling
            MultiplayerClientEvent.RoomGameplaySettingsChanged.eventName -> MultiplayerServerEvent.RoomGameplaySettingsChanged.eventName to arrayOf((args[0] as JSONObject).apply {
                Multiplayer.room!!.gameplaySettings.also {
                    if (!has("isFreeMod")) put("isFreeMod", it.isFreeMod)
                    if (!has("isRemoveSliderLock")) put("isRemoveSliderLock", it.isRemoveSliderLock)
                }
            })

            else -> event to args
        }


        val (key, arguments) = responseEvent

        listeners(key).forEach {
            it.call(*arguments)
        }
    }


    override fun connect(): Socket {
        isConnected = true

        emit(EVENT_CONNECT)

        // This event is emitted by the server when the client connects.
        emit(MultiplayerServerEvent.InitialConnection.eventName, JSONObject().apply {

            put("id", 1)
            put("name", "Test room")
            put("isLocked", false)
            put("maxPlayers", 8)
            put("teamMode", TeamMode.HeadToHead.ordinal)
            put("winCondition", WinCondition.ScoreV1.ordinal)
            put("playerCount", 1)
            put("playerNames", Config.getOnlineUsername())
            put("sessionId", "")
            put("status", RoomStatus.Idle.ordinal)
            put("beatmap", null)

            put("roundId", currentRoundId)
            put("latestChatId", currentChatId)

            put("host", JSONObject().apply {
                put("id", uid)
            })

            put("mods", JSONArray())

            put("gameplaySettings", JSONObject().apply {
                put("isFreeMod", true)
                put("isRemoveSliderLock", false)
            })

            put("players", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", uid)
                    put("username", Config.getOnlineUsername())
                    put("rank", 1)
                    put("status", PlayerStatus.NotReady.ordinal)
                    put("team", null)
                    put("mods", JSONArray())
                })
            })

        })

        return this
    }

    override fun disconnect(): Socket {
        isConnected = false

        emit(EVENT_DISCONNECT, "io server disconnect")
        return this
    }

}