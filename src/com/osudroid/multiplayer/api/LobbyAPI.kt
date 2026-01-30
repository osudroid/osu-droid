package com.osudroid.multiplayer.api

import com.osudroid.BuildSettings
import com.osudroid.debug.MockRoom
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.parseGameplaySettings
import com.reco1l.framework.net.JsonArrayRequest
import com.reco1l.framework.net.JsonObjectRequest
import com.reco1l.toolkt.data.putObject

object LobbyAPI {

    /**
     * The hostname.
     */
    const val HOST = "https://multi.osudroid.moe"

    /**
     * The invite link host.
     */
    const val INVITE_HOST = "https://odmp"


    /**
     * Endpoint to get a rooms list.
     */
    private const val GET_ROOMS = "/getrooms"

    /**
     * Endpoint to create a room.
     */
    private const val CREATE_ROOM = "/createroom"


    /**
     * Get room list.
     */
    fun getRooms(query: String?, uid: Long, sessionId: String, sign: String?): List<Room> {

        if (BuildSettings.MOCK_MULTIPLAYER) {
            return listOf(
                MockRoom(),
                MockRoom(),
                MockRoom()
            )
        }

        JsonArrayRequest("$HOST$GET_ROOMS").use {

            if (sign != null || query != null) {
                it.buildUrl {
                    addQueryParameter("sign", sign)
                    addQueryParameter("query", query)
                    addQueryParameter("uid", uid.toString())
                    addQueryParameter("sessionId", sessionId)
                }
            }

            return List(it.execute().json.length()) { i ->

                val json = it.json.optJSONObject(i)

                return@List try {

                    Room(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        isLocked = json.getBoolean("isLocked"),
                        maxPlayers = json.getInt("maxPlayers"),
                        mods = RoomMods(json.getJSONArray("mods")),
                        gameplaySettings = parseGameplaySettings(json.getJSONObject("gameplaySettings")),
                        teamMode = TeamMode[json.getInt("teamMode")],
                        winCondition = WinCondition.from(json.getInt("winCondition")),
                        playerCount = json.getInt("playerCount"),
                        playerNames = json.getString("playerNames"),
                        status = RoomStatus[json.getInt("status")]
                    )

                } catch (_: Exception) {
                    null
                }

            }.filterNotNull()
        }
    }

    /**
     * Create room and get the ID.
     */
    fun createRoom(name: String, beatmap: RoomBeatmap?, hostUID: Long, sessionId: String, sign: String?, password: String? = null, maxPlayers: Int = 8): Long {

        if (BuildSettings.MOCK_MULTIPLAYER) {
            return 1
        }

        JsonObjectRequest("$HOST$CREATE_ROOM").use { request ->

            request.buildRequestBody {

                put("hostUid", hostUID)
                put("name", name)
                put("maxPlayers", maxPlayers)

                if (beatmap != null) {
                    putObject("beatmap") {
                        put("md5", beatmap.md5)
                        put("title", beatmap.title)
                        put("artist", beatmap.artist)
                        put("creator", beatmap.creator)
                        put("version", beatmap.version)
                    }
                }

                if (!password.isNullOrBlank()) {
                    put("password", password)
                }

                put("version", RoomAPI.API_VERSION)
                put("sessionId", sessionId)
                put("sign", sign)
            }

            return request.execute().json.getString("id").toLong()
        }
    }
}