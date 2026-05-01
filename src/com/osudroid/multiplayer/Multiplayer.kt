package com.osudroid.multiplayer

import android.net.*
import android.util.Log
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomPlayer
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard
import com.osudroid.ui.v2.multi.*
import com.reco1l.andengine.*
import com.osudroid.utils.updateThread
import com.reco1l.toolkt.kotlin.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.*
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

object Multiplayer {

    /**
     * The current room scene.
     */
    @JvmField
    var roomScene: RoomScene? = null

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
    @Volatile
    var isMultiplayer = false

    /**
     * Indicates if the client is waiting for a reconnection.
     */
    @JvmField
    @Volatile
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
     * active; it can be under reconnection state.
     *
     * Note: Any event emitted during reconnection is ignored.
     */
    @JvmStatic
    val isConnected
        get() = room != null

    private val logger = MultiplayerLogger()

    /**
     * [CoroutineScope] used for reconnection.
     */
    private var reconnectionScope: CoroutineScope? = null

    /**
     * Active reconnection coroutine [Job].
     */
    private var reconnectionJob: Job? = null

    @Volatile
    private var attemptCount = 0

    @Volatile
    private var reconnectionStartTimeMS = 0L

    @Volatile
    private var lastAttemptResponseTimeMS = 0L

    @Volatile
    private var isWaitingAttemptResponse = false

    private val abandonReconnectionLock = Any()

    //region Connection

    @JvmStatic
    fun connectFromLink(link: Uri) {

        if (isConnected) {
            return
        }

        GlobalManager.getInstance().songService.isGaming = true
        isMultiplayer = true

        async {

            try {
                LoadingScreen().show()

                GlobalManager.getInstance().mainActivity.checkNewSkins()
                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                val roomID = link.pathSegments[0].toLong()
                val password = if (link.pathSegments.size > 1) link.pathSegments[1] else null

                RoomAPI.connectToRoom(
                    roomId = roomID,
                    userId = OnlineManager.getInstance().userId,
                    gameSessionId = OnlineManager.getInstance().sessionId,
                    roomPassword = password
                )

            } catch (e: Exception) {
                ToastLogger.showText("Failed to connect to the room: ${e.javaClass} - ${e.message}", true)
                Log.e("LobbyScene", "Failed to connect to room.", e)

                UIEngine.current.scene = LobbyScene()
            }

        }
    }

    //endregion


    // Leaderboard

    fun onLiveLeaderboard(array: JSONArray) {

        if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
            return
        }

        val hud = GlobalManager.getInstance().gameScene.hud ?: return

        if (hud.hasElement(HUDLeaderboard::class)) {

            val itemsList = MutableList(array.length()) { i ->
                jsonToScoreboardItem(array.getJSONObject(i)).apply { rank = i + 1 }
            }

            hud.forEachElement { element ->
                if (element is HUDLeaderboard) {
                    element.nextItems = itemsList
                }
            }
        }
    }

    fun onFinalLeaderboard(array: JSONArray) {

        finalData = null

        // Avoiding data parsing if user left from ScoringScene
        if (GlobalManager.getInstance().engine.scene == roomScene || GlobalManager.getInstance().engine.scene == GlobalManager.getInstance().songMenu.scene) {
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

        if (list.isEmpty()) {
            return
        }

        // Replace server statistics with local in Head-to-Head mode.
        if (room?.isTeamVersus == false) {
            val ownScore = GlobalManager.getInstance().gameScene.stat
            val ownScoreIndex = list.indexOfFirst { it.playerName == OnlineManager.getInstance().username }.takeUnless { it == -1 }

            if (ownScore != null) {
                if (ownScoreIndex == null) {
                    // This should never happen, but if the server leaderboard doesn't include the
                    // local player (e.g. username casing mismatch or API mismatch), append the local
                    // score so it remains visible rather than being silently omitted.
                    list.add(ownScore)
                    log("WARNING: Player score wasn't found in final leaderboard.")
                } else {
                    list[ownScoreIndex] = ownScore
                }
            }
        }

        finalData = list.toTypedArray()

        // Reloading results screen
        GlobalManager.getInstance().scoring.updateLeaderboard()
    }


    // Reconnection

    /**
     * Cancels any active reconnection coroutine state. Should be called when leaving the room.
     */
    fun cancelReconnection() {
        isReconnecting = false
        reconnectionJob?.cancel()
        reconnectionJob = null
        reconnectionScope = null
    }

    /**
     * Permanently gives up on reconnecting: cancels the reconnection loop, shows the error
     * toast, and navigates back to the lobby (unless gameplay is currently active, in which
     * case the navigation is deferred to the game-end flow).
     *
     * Idempotent — if a second caller races in after the first has already set
     * [isReconnecting] to `false`, it is a no-op. This ensures the two independent
     * exit paths (30-second absolute timeout in the coroutine loop and the max-attempt
     * guard in [onReconnectAttempt]) cannot both execute the toast + `back()` sequence.
     */
    private fun abandonReconnection() {
        synchronized(abandonReconnectionLock) {
            if (!isReconnecting) return
            isReconnecting = false

            reconnectionJob?.cancel()
            reconnectionJob = null

            ToastLogger.showText(
                "The connection to server has been lost, please check your internet connection.",
                true
            )

            // Do not interrupt an active game session; the teardown will happen naturally
            // when the player finishes (or abandons) the game.
            val gameScene = GlobalManager.getInstance().gameScene
            if (gameScene == null || GlobalManager.getInstance().engine.scene != gameScene.scene) {
                updateThread { roomScene?.back() }
            }
        }
    }

    fun onReconnectAttempt(success: Boolean) {

        lastAttemptResponseTimeMS = System.currentTimeMillis()

        if (success) {
            isReconnecting = false

            roomScene?.chat?.onSystemChatMessage("Connection was successfully restored.", "#459FFF")
        } else if (attemptCount < 5) {
            attemptCount++

            roomScene?.chat?.onSystemChatMessage("Failed to reconnect, trying again in 5 seconds...", "#FFBFBF")
        } else {
            // Max attempts reached — delegate to the shared exit path.
            abandonReconnection()
        }

        isWaitingAttemptResponse = false
    }

    fun onReconnect() {
        if (isReconnecting) {
            log("WARNING: onReconnect() called while already trying to reconnect.")
            return
        }
        isReconnecting = true

        // Cancel any leftover job from a previous session and create a fresh scope with an
        // explicit SupervisorJob so the scope itself can be canceled if needed.
        reconnectionJob?.cancel()
        reconnectionJob = null

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        reconnectionScope = scope

        attemptCount = 0
        reconnectionStartTimeMS = System.currentTimeMillis()

        reconnectionJob = scope.launch {

            while (isReconnecting) {
                val currentTime = System.currentTimeMillis()

                // Absolute timeout exceeded — delegate to the shared exit path.
                if (currentTime - reconnectionStartTimeMS >= 30000) {
                    abandonReconnection()
                    return@launch
                }

                // Still waiting for the server to respond to the last attempt — poll cheaply
                // instead of spinning at 100% CPU.
                if (isWaitingAttemptResponse) {
                    delay(250.milliseconds)
                    continue
                }

                // Inter-attempt cooldown: sleep for the exact remaining time rather than
                // busy-spinning until the 5-second window has elapsed.
                val msSinceLastResponse = currentTime - lastAttemptResponseTimeMS
                if (msSinceLastResponse < 5000) {
                    delay((5000 - msSinceLastResponse).milliseconds)
                    continue
                }

                try {
                    RoomAPI.connectToRoom(
                        roomId = room!!.id,
                        userId = OnlineManager.getInstance().userId,
                        gameSessionId = OnlineManager.getInstance().sessionId,
                        multiplayerSessionID = room!!.sessionID
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
        logger.log(text)
    }

    @JvmStatic
    fun log(e: Throwable) {
        logger.log(e)
    }

    @JvmStatic
    fun flushLog() {
        logger.flush()
    }
}