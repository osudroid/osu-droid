package com.reco1l.api.ibancho

import com.reco1l.api.ibancho.data.*
import com.reco1l.framework.extensions.orCatch
import com.reco1l.framework.net.JsonContent
import com.reco1l.framework.net.JsonRequester
import com.reco1l.framework.net.QueryContent

object LobbyAPI
{
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
    fun getRooms(query: String?, sign: String?): List<Room>
    {
        JsonRequester("$HOST$GET_ROOMS").use {

            it.log = false

            if (sign != null || query != null)
                it.query = QueryContent().apply {
                    put("sign", sign)
                    put("query", query)
                }

            val array = it.executeAndGetJson().toArray() ?: return emptyList()

            return List(array.length()) { i ->

                val json = array.optJSONObject(i)

                return@List {

                    Room(
                            id = json.getLong("id"),
                            name = json.getString("name"),
                            isLocked = json.getBoolean("isLocked"),
                            maxPlayers = json.getInt("maxPlayers"),
                            mods = json.getString("mods"),
                            isFreeMods = json.getBoolean("isFreeMod"),
                            teamMode = TeamMode.from(json.getInt("teamMode")),
                            winCondition = WinCondition.from(json.getInt("winCondition")),
                            playerCount = json.getInt("playerCount"),
                            playerNames = json.getString("playerNames"),
                            status = RoomStatus.from(json.getInt("status"))
                    )

                }.orCatch { null }

            }.filterNotNull()
        }
    }

    /**
     * Create room and get the ID.
     */
    fun createRoom(name: String, beatmap: RoomBeatmap?, hostUID: Long, hostUsername: String, sign: String?, password: String? = null, maxPlayers: Int = 8): Long
    {
        JsonRequester("$HOST$CREATE_ROOM").use { request ->

            request.jsonInsertion = JsonContent().apply {

                put("name", name)
                put("maxPlayers", maxPlayers)

                putGroup("host")
                        .put("uid", hostUID.toString())
                        .put("username", hostUsername)

                if (beatmap != null)
                {
                    putGroup("beatmap")
                            .put("md5", beatmap.md5)
                            .put("title", beatmap.title)
                            .put("artist", beatmap.artist)
                            .put("creator", beatmap.creator)
                            .put("version", beatmap.version)
                }

                if (!password.isNullOrBlank())
                    put("password", password)

                put("sign", sign)
            }

            return request.executeAndGetJson().getString("id").toLong()
        }
    }
}