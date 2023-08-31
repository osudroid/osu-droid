package com.reco1l.api.ibancho

import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.framework.net.JsonContent
import com.reco1l.framework.net.JsonRequester
import com.reco1l.legacy.ui.multiplayer.multiLog
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object SpectatorAPI {
    private const val MAIN_ENDPOINT = OnlineManager.endpoint + "/"
    private const val START_PLAYING = "startPlaying"

    private const val EVENTS_ENDPOINT = "${OnlineManager.endpoint}events/"
    private const val CHANGE_BEATMAP = "changeBeatmap"
    private const val JOIN_ROOM = "playerJoined"
    private const val LEAVE_ROOM = "playerLeft"

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

    fun joinRoom(roomId: Long, uid: Long) {
        JsonRequester("$EVENTS_ENDPOINT$JOIN_ROOM").use {
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
}
