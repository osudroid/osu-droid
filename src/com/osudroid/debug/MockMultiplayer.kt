package com.osudroid.debug

import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.toolkt.data.*
import com.reco1l.toolkt.kotlin.*
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

    override fun emit(event: String?, vararg args: Any?): Emitter {

        // Emulating the server response to the client's events.
        val responseEvent = when (event) {

            // In these events the client expects a JSONArray as a response.
            "scoreSubmission" -> "allPlayersScoreSubmitted" to arrayOf(JSONArray().apply { put(args[0]) })
            "liveScoreData" -> "liveScoreData" to arrayOf(JSONArray().apply {
                // The username is expected to be added by the server.
                put((args[0] as JSONObject).put("username", Config.getOnlineUsername()))
            })

            // Simulating the server response when all players emit these events.
            "beatmapLoadComplete" -> "allPlayersBeatmapLoadComplete" to arrayOf()
            "skipRequested" -> "allPlayersSkipRequested" to arrayOf()

            // In these events the client expects to receive the uid of the player as the first argument.
            "chatMessage" -> "chatMessage" to arrayOf(uid.toString(), args[0])
            "teamChanged" -> "teamChanged" to arrayOf(uid.toString(), args[0])
            "playerModsChanged" -> "playerModsChanged" to arrayOf(uid.toString(), args[0])
            "playerStatusChanged" -> "playerStatusChanged" to arrayOf(uid.toString(), args[0])

            // In these events, the client expects to receive an integer as the first argument.
            "maxPlayersChanged" -> "maxPlayersChanged" to arrayOf(args[0].toString())

            // These events require special handling
            "roomGameplaySettingsChanged" -> "roomGameplaySettingsChanged" to arrayOf((args[0] as JSONObject).apply {
                Multiplayer.room!!.gameplaySettings.also {
                    if (!has("isFreeMod")) put("isFreeMod", it.isFreeMod)
                    if (!has("isRemoveSliderLock")) put("isRemoveSliderLock", it.isRemoveSliderLock)
                }
            })

            else -> event to args
        }


        val (key, arguments) = responseEvent

        listeners(key).fastForEach {
            it.call(*arguments)
        }

        return this
    }


    override fun connect(): Socket {
        emit(EVENT_CONNECT)

        // This event is emitted by the server when the client connects.
        emit("initialConnection", JSONObject().apply {

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

            putObject("host") {
                put("uid", uid)
            }

            put("mods", JSONArray())

            putObject("gameplaySettings") {
                put("isFreeMod", true)
                put("isRemoveSliderLock", false)
            }

            putArray("players") {

                putObject {
                    put("uid", uid)
                    put("username", Config.getOnlineUsername())
                    put("status", PlayerStatus.NotReady.ordinal)
                    put("team", null)
                    put("mods", JSONArray())
                }

            }

        })

        return this
    }

    override fun disconnect(): Socket {
        emit(EVENT_DISCONNECT, "io server disconnect")
        return this
    }

}