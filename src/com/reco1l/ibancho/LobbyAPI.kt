package com.reco1l.ibancho

import com.reco1l.framework.net.JsonArrayRequest
import com.reco1l.framework.net.JsonObjectRequest
import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomBeatmap
import com.reco1l.ibancho.data.RoomStatus
import com.reco1l.ibancho.data.TeamMode
import com.reco1l.ibancho.data.WinCondition
import com.reco1l.ibancho.data.parseGameplaySettings
import com.reco1l.ibancho.data.parseMods
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
    fun getRooms(query: String?, sign: String?): List<Room> {

        JsonArrayRequest("$HOST$GET_ROOMS").use {

            if (sign != null || query != null) {
                it.buildUrl {
                    addQueryParameter("sign", sign)
                    addQueryParameter("query", query)
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
                        mods = parseMods(json.getJSONObject("mods")),
                        gameplaySettings = parseGameplaySettings(json.getJSONObject("gameplaySettings")),
                        teamMode = TeamMode[json.getInt("teamMode")],
                        winCondition = WinCondition.from(json.getInt("winCondition")),
                        playerCount = json.getInt("playerCount"),
                        playerNames = json.getString("playerNames"),
                        status = RoomStatus[json.getInt("status")]
                    )

                } catch (e: Exception) {
                    null
                }

            }.filterNotNull()
        }
    }

    /**
     * Create room and get the ID.
     */
    fun createRoom(name: String, beatmap: RoomBeatmap?, hostUID: Long, hostUsername: String, sign: String?, password: String? = null, maxPlayers: Int = 8): Long {

        JsonObjectRequest("$HOST$CREATE_ROOM").use { request ->

            request.buildRequestBody {

                put("name", name)
                put("maxPlayers", maxPlayers)

                putObject("host") {
                    put("uid", hostUID.toString())
                    put("username", hostUsername)
                }

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

                put("sign", sign)
            }

            return request.execute().json.getString("id").toLong()
        }
    }
}