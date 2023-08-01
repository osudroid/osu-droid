package com.reco1l.api.ibancho

import com.reco1l.api.ibancho.data.*
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.iterator
import com.reco1l.framework.extensions.logWithMessage
import com.reco1l.framework.net.JsonContent
import com.reco1l.framework.net.JsonRequester
import com.reco1l.framework.net.QueryContent
import org.json.JSONObject

object LobbyAPI
{
    /**
     * The hostname.
     */
    const val HOST = "https://multi.osudroid.moe"

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
    fun getRooms(query: String?, sign: String): List<Room>
    {
        JsonRequester("$HOST$GET_ROOMS").use {

            //it.log = false

            it.query = QueryContent().apply {
                put("sign", sign)

                if (query != null)
                    put("query", query)
            }

            val list = mutableListOf<Room>()
            val array = it.executeAndGetJson().toArray() ?: return list

            for (data in array)
            {
                val json = data as JSONObject

                try
                {
                    val room = Room(
                            id = json.getLong("id"),
                            name = json.getString("name"),
                            isLocked = json.getBoolean("isLocked"),
                            maxPlayers = json.getInt("maxPlayers"),
                            mods = json.getString("mods"),
                            isFreeMods = json.getBoolean("isFreeMod"),
                            teamMode = TeamMode.from(json.getInt("teamMode")),
                            winCondition = WinCondition.from(json.getInt("winCondition")),
                            playerCount = json.getInt("playerCount"),
                            playerNames = json.getString("playerNames")
                    )
                    list.add(room)
                }
                catch (e: Exception)
                {
                    e.logWithMessage(className) { "Failed to parse room" }
                }
            }
            return list
        }
    }

    /**
     * Create room and get the ID.
     */
    fun createRoom(name: String, beatmap: RoomBeatmap?, hostUID: Long, hostUsername: String, sign: String, password: String? = null, maxPlayers: Int = 8) : Long
    {
        JsonRequester("$HOST$CREATE_ROOM").use { request ->

            request.jsonInsertion = JsonContent().apply {

                put("name", name)
                put("maxPlayers", maxPlayers)

                putGroup("host")
                        .put("uid", hostUID.toString())
                        .put("username", hostUsername)

                if (beatmap != null)
                    put("beatmap", JSONObject(beatmap.toString()))

                if (!password.isNullOrBlank())
                    put("password", password)

                put("sign", sign)
            }

            return request.executeAndGetJson().getString("id").toLong()
        }
    }
}