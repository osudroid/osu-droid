package com.reco1l.osu.multiplayer

import android.text.format.DateFormat
import android.util.Log
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomPlayer
import com.reco1l.toolkt.kotlin.formatTimeMilliseconds
import com.reco1l.toolkt.kotlin.fromDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.MainActivity
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.io.File

object Multiplayer {

    /**
     * The current room, it can become `null` if socket disconnects.
     */
    @JvmField
    var room: Room? = null

    /**
     * The player that correspond us according to client UID, it can become `null` if socket disconnects.
     */
    @JvmField
    var player: RoomPlayer? = null

    /**
     * Indicates if player is in multiplayer mode
     */
    @JvmField
    var isMultiplayer = false

    /**
     * Indicates if the client is waiting for a reconnection.
     */
    @JvmField
    var isReconnecting = false

    /**
     * Array containing final leaderboard
     */
    @JvmField
    var finalData: Array<StatisticV2>? = null


    /**
     * Indicates if player is room host
     */
    @JvmStatic
    val isRoomHost
        get() = player?.let { it.id == room?.host } ?: false

    /**
     * Determines if the player is connected to a room. This doesn't ensure that the connection to socket is currently
     * active it can be under reconnection state.
     *
     * Note: Any event emitted during reconnection is ignored.
     */
    @JvmStatic
    val isConnected
        get() = room != null


    private val LOG_FILE = File("${Config.getDefaultCorePath()}/Log", "multi_log.txt").apply {

        parentFile?.mkdirs()

        if (!exists()) {
            createNewFile()
        }
    }

    private val reconnectionScope by lazy { CoroutineScope(Dispatchers.Default) }


    private var attemptCount = 0

    private var reconnectionStartTimeMS = 0L

    private var lastAttemptResponseTimeMS = 0L

    private var isWaitingAttemptResponse = false


    init {
        LOG_FILE.writeText("[${"yyyy/MM/dd hh:mm:ss".fromDate()}] Client ${MainActivity.versionName} started.")
    }


    // Leaderboard

    fun onLiveLeaderboard(array: JSONArray) {

        if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
            return
        }

        GlobalManager.getInstance().gameScene.scoreBoard?.nextItems = MutableList(array.length()) { i ->
            val json = array.getJSONObject(i)

            jsonToScoreboardItem(json).apply { rank = i + 1 }
        }
    }

    fun onFinalLeaderboard(array: JSONArray) {

        finalData = null

        // Avoiding data parsing if user left from ScoringScene
        if (GlobalManager.getInstance().engine.scene == RoomScene || GlobalManager.getInstance().engine.scene == GlobalManager.getInstance().songMenu.scene) {
            return
        }

        if (array.length() == 0) {
            log("WARNING: Server provided empty final leaderboard.")
            return
        }

        val list = mutableListOf<StatisticV2>()

        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            list.add(jsonToStatistic(json))
        }

        if (list.isEmpty()) return

        // Replacing server statistic with local
        val ownScore = GlobalManager.getInstance().gameScene.stat
        val ownScoreIndex = list.indexOfFirst { it.playerName == OnlineManager.getInstance().username }.takeUnless { it == -1 }

        if (ownScore != null) {
            // This should never happen
            if (ownScoreIndex == null) {
                list.add(ownScore)
                log("WARNING: Player score wasn't found in final leaderboard.")
            } else {
                list[ownScoreIndex] = ownScore
            }
        }

        finalData = list.toTypedArray()

        // Reloading results screen
        GlobalManager.getInstance().scoring.updateLeaderboard()
    }


    // Reconnection

    fun onReconnectAttempt(success: Boolean) {

        lastAttemptResponseTimeMS = System.currentTimeMillis()

        if (success) {
            isReconnecting = false

            RoomScene.chat.onSystemChatMessage("Connection was successfully restored.", "#459FFF")
        } else if (attemptCount < 5) {
            attemptCount++

            RoomScene.chat.onSystemChatMessage("Failed to reconnect, trying again in 5 seconds...", "#FFBFBF")
        } else {
            isReconnecting = false

            ToastLogger.showText("The connection to server has been lost, please check your internet connection.", true)
            RoomScene.back()
        }

        isWaitingAttemptResponse = false
    }

    fun onReconnect() {
        if (isReconnecting) {
            log("WARNING: onReconnect() called while already trying to reconnect.")
            return
        }
        isReconnecting = true

        attemptCount = 0
        reconnectionStartTimeMS = System.currentTimeMillis()

        reconnectionScope.launch {

            while (isReconnecting) {
                val currentTime = System.currentTimeMillis()

                // Timeout to reconnect was exceed.
                if (currentTime - reconnectionStartTimeMS >= 30000) {
                    ToastLogger.showText("The connection to server has been lost, please check your internet connection.", true)
                    RoomScene.back()
                    return@launch
                }

                if (currentTime - lastAttemptResponseTimeMS < 5000 || isWaitingAttemptResponse) continue

                try {
                    RoomAPI.connectToRoom(
                        roomId = room!!.id,
                        userId = OnlineManager.getInstance().userId,
                        username = OnlineManager.getInstance().username,
                        sessionID = room!!.sessionID
                    )

                    isWaitingAttemptResponse = true
                } catch (e: Exception) {
                    log(e)

                    // In this case the client didn't even succeed while creating the socket.
                    onReconnectAttempt(false)
                }

            }
        }
    }


    // Logging

    @JvmStatic
    fun log(text: String) {
        val timestamp = DateFormat.format("hh:mm:ss", System.currentTimeMillis())

        LOG_FILE.appendText("\n[$timestamp] $text")
        Log.i("Multiplayer", text)
    }

    @JvmStatic
    fun log(e: Throwable) {
        val time = "hh:mm:ss".formatTimeMilliseconds(System.currentTimeMillis())
        val stacktrace = Log.getStackTraceString(e)

        LOG_FILE.appendText("\n[$time] EXCEPTION: ${e.javaClass.simpleName}\n$stacktrace")

        Log.e("Multiplayer", "An exception has been thrown.", e)
    }
}