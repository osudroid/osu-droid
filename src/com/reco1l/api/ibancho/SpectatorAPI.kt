package com.reco1l.api.ibancho

import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.api.ibancho.data.RoomPlayer
import com.reco1l.api.ibancho.data.RoomTeam
import com.reco1l.api.ibancho.data.TeamMode
import com.reco1l.framework.net.JsonContent
import com.reco1l.framework.net.JsonRequester
import com.reco1l.legacy.data.modsToString
import com.reco1l.legacy.ui.multiplayer.RoomMods
import com.reco1l.legacy.ui.multiplayer.multiLog
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object SpectatorAPI {
    private const val MAIN_ENDPOINT = OnlineManager.endpoint + "/"
    private const val START_PLAYING = "startPlaying"

    private const val EVENTS_ENDPOINT = "${OnlineManager.endpoint}/events/"
    private const val CHANGE_BEATMAP = "changeBeatmap"
    private const val JOIN_ROOM = "playerJoined"
    private const val LEAVE_ROOM = "playerLeft"
    private const val CHANGE_MODS = "modsChange"
    private const val CHANGE_TEAM = "playerTeamChange"
    private const val CHANGE_TEAM_MODE = "teamModeChange"

    fun startPlaying(roomId: Long) {
        JsonRequester("$MAIN_ENDPOINT$START_PLAYING").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId)
                put("sign", SecurityUtils.signRequest(roomId.toString()))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }

    fun changeBeatmap(roomId: Long, md5: String) {
        JsonRequester("$EVENTS_ENDPOINT$CHANGE_BEATMAP").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId)
                put("hash", md5)
                put("sign", SecurityUtils.signRequest("$roomId$md5"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }

    fun joinRoom(roomId: Long, player: RoomPlayer) {
        JsonRequester("$EVENTS_ENDPOINT$JOIN_ROOM").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId)

                // Putting these here to strengthen sign.
                put("uid", player.id)
                put("username", player.name)
                put("team", player.team)

                putGroup("player")
                    .put("uid", player.id)
                    .put("username", player.name)
                    .put("team", player.team)

                put("sign", SecurityUtils.signRequest("$roomId${player.id}${player.name}${player.team}"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }

    fun leaveRoom(roomId: Long, uid: Long) {
        JsonRequester("$EVENTS_ENDPOINT$LEAVE_ROOM").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId)
                put("uid", uid)
                put("sign", SecurityUtils.signRequest("$roomId$uid"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }

    fun changeMods(roomId: Long, mods: RoomMods) {
        JsonRequester("$EVENTS_ENDPOINT$CHANGE_MODS").use {
            it.jsonInsertion = JsonContent().apply {
                val modString = modsToString(mods.set)

                put("roomId", roomId)
                put("mods", modString)
                put("speedMultiplier", mods.speedMultiplier)

                put("sign", SecurityUtils.signRequest("$roomId$modString${mods.speedMultiplier}"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }

    fun changeTeam(roomId: Long, uid: Long, team: RoomTeam) {
        JsonRequester("$EVENTS_ENDPOINT$CHANGE_TEAM").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId)
                put("uid", uid)
                put("team", team.ordinal)

                put("sign", SecurityUtils.signRequest("$roomId$uid${team.ordinal}"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }

    fun changeTeamMode(roomId: Long, teamMode: TeamMode) {
        JsonRequester("$EVENTS_ENDPOINT$CHANGE_TEAM_MODE").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId)
                put("mode", teamMode.ordinal)

                put("sign", SecurityUtils.signRequest("$roomId${teamMode.ordinal}"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }
}
