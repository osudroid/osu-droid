@file:JvmName("BuildUtils")

package com.reco1l

import com.reco1l.ibancho.data.*
import com.reco1l.osu.multiplayer.*
import com.reco1l.toolkt.data.*
import com.reco1l.toolkt.kotlin.*
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.game.mods.*
import ru.nsu.ccfit.zuev.osu.menu.*
import java.util.*

/**
 * Whether to use textures or not.
 */
const val noTexturesMode = false

/**
 * Whether to keep the shape of the textures if no texture mode is enabled.
 *
 * Note: This can increase texture loading time.
 */
const val keepTexturesShapeInNoTexturesMode = false

/**
 * Whether to use fake multiplayer mode or not.
 */
const val fakeMultiplayerMode = false


// Fake multiplayer mode

fun generateFakeRoom() = Room(
    id = 1,
    name = "Test Room",
    isLocked = false,
    maxPlayers = 8,
    mods = RoomMods(
        set = EnumSet.noneOf(GameMod::class.java),
        speedMultiplier = 1f,
        flFollowDelay = ModMenu.DEFAULT_FL_FOLLOW_DELAY,
        customAR = null,
        customOD = null,
        customCS = null,
        customHP = null
    ),
    gameplaySettings = RoomGameplaySettings(
        isFreeMod = true,
        isRemoveSliderLock = false,
        allowForceDifficultyStatistics = true
    ),
    teamMode = TeamMode.HeadToHead,
    winCondition = WinCondition.ScoreV1,
    playerCount = 0,
    playerNames = "",
    status = RoomStatus.Idle
)

class FakeSocket(private val uid: Long, private val username: String) : Socket(null, null, null) {

    override fun emit(event: String?, vararg args: Any?): Emitter {

        // Emulating the server response to the client's events.
        val responseEvent = when (event) {

            // In these events the client expects a JSONArray as a response.
            "scoreSubmission" -> "allPlayersScoreSubmitted" to arrayOf(JSONArray().apply { put(args[0]) })
            "liveScoreData" -> "liveScoreData" to arrayOf(JSONArray().apply {
                // The username is expected to be added by the server.
                put((args[0] as JSONObject).put("username", username))
            })

            // Simulating the server response when all players emit these events.
            "beatmapLoadComplete" -> "allPlayersBeatmapLoadComplete" to arrayOf()
            "skipRequested" -> "allPlayersSkipRequested" to arrayOf()

            // In these events the client expects to receive the uid of the player as the first argument.
            "chatMessage" -> "chatMessage" to arrayOf(uid.toString(), args[0])
            "teamChanged" -> "teamChanged" to arrayOf(uid.toString(), args[0])
            "playerModsChanged" -> "playerModsChanged" to arrayOf(uid.toString(), args[0])
            "playerStatusChanged" -> "playerStatusChanged" to arrayOf(uid.toString(), args[0])

            // These events requires special handling
            "roomGameplaySettingsChanged" -> "roomGameplaySettingsChanged" to arrayOf((args[0] as JSONObject).apply {
                Multiplayer.room!!.gameplaySettings.also {
                    if (!has("isFreeMod")) put("isFreeMod", it.isFreeMod)
                    if (!has("isRemoveSliderLock")) put("isRemoveSliderLock", it.isRemoveSliderLock)
                    if (!has("allowForceDifficultyStatistics")) put("allowForceDifficultyStatistics", it.allowForceDifficultyStatistics)
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

        // This event is emmited by the server when the client connects.
        emit("initialConnection", JSONObject().apply {

            put("id", 1)
            put("name", "Test room")
            put("isLocked", false)
            put("maxPlayers", 8)
            put("teamMode", TeamMode.HeadToHead.ordinal)
            put("winCondition", WinCondition.ScoreV1.ordinal)
            put("playerCount", 1)
            put("playerNames", username)
            put("sessionId", "")
            put("status", RoomStatus.Idle.ordinal)
            put("beatmap", null)

            putObject("host") {
                put("uid", uid)
            }

            putObject("mods") {
                put("mods", "")
                put("speedMultiplier", 1f)
                put("flFollowDelay", ModMenu.DEFAULT_FL_FOLLOW_DELAY)
                put("customAR", null)
                put("customOD", null)
                put("customCS", null)
                put("customHP", null)
            }

            putObject("gameplaySettings") {
                put("isFreeMod", true)
                put("isRemoveSliderLock", false)
                put("allowForceDifficultyStatistics", true)
            }

            putArray("players") {

                putObject {
                    put("uid", uid)
                    put("username", username)
                    put("status", PlayerStatus.NotReady.ordinal)
                    put("team", null)
                    put("mods", JSONObject().apply {
                        put("mods", "")
                        put("speedMultiplier", 1f)
                        put("flFollowDelay", ModMenu.DEFAULT_FL_FOLLOW_DELAY)
                        put("customAR", null)
                        put("customOD", null)
                        put("customCS", null)
                        put("customHP", null)
                    })
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