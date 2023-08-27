package com.reco1l.api.ibancho

import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.framework.net.JsonContent
import com.reco1l.framework.net.JsonRequester
import com.reco1l.legacy.ui.multiplayer.multiLog
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object SpectatorAPI {
    private const val endpoint = "${OnlineManager.endpoint}/events/"
    private const val changeBeatmap = "changeBeatmap"

    fun changeBeatmap(roomId: Long, md5: String) {
        JsonRequester("$endpoint$changeBeatmap").use {
            it.jsonInsertion = JsonContent().apply {
                put("roomId", roomId.toString())
                put("hash", md5)
                put("key", SecurityUtils.signRequest("$roomId$md5"))
            }

            try {
                it.execute()
            } catch (e: Exception) {
                multiLog(e)
            }
        }
    }
}