package com.osudroid.multiplayer.api

import com.osudroid.BuildSettings
import com.osudroid.debug.MockSocket
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.data.MultiplayerMessage
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.parseBeatmap
import com.osudroid.multiplayer.api.data.parseGameplaySettings
import com.osudroid.multiplayer.api.data.parsePlayer
import com.osudroid.multiplayer.api.data.parsePlayers
import com.osudroid.multiplayer.session.toMultiplayerRecoveryPayload
import com.osudroid.ui.v2.multi.RoomScene
import com.osudroid.utils.updateThread
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter.Listener
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import java.util.UUID

object RoomAPI {

    /**
     * The API version.
     */
    const val API_VERSION = 10


    /**
     * The listener for player events.
     */
    var playerEventListener: IPlayerEventListener? = null

    /**
     * The listener for room events.
     */
    var roomEventListener: IRoomEventListener? = null


    @Volatile
    private var socket: Socket? = null

    @Volatile
    internal var lastRoomPassword: String? = null

    /**
     * The transport the [Multiplayer.session]'s outbox sends events through.
     */
    internal val transport = MultiplayerSocketTransport { socket }


    // https://gist.github.com/Rian8337/ceab4d3b179cbeee7dd548cfcf145b95
    // Back-to-back events

    private val beatmapChanged = Listener {
        Multiplayer.log("RECEIVED: beatmapChanged -> ${it.contentToString()}")

        val json = it[0] as? JSONObject
        val beatmap = parseBeatmap(json)

        roomEventListener?.onRoomBeatmapChange(beatmap)
    }

    private val hostChanged = Listener {

        Multiplayer.log("RECEIVED: hostChanged -> ${it.contentToString()}")
        val uid = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: hostChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomHostChange(uid)
    }

    private val playerKicked = Listener {

        Multiplayer.log("RECEIVED: playerKicked -> ${it.contentToString()}")
        val uid = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerKicked — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        playerEventListener?.onPlayerKick(uid)
    }

    private val playerModsChanged = Listener {
        Multiplayer.log("RECEIVED: playerModsChanged -> ${it.contentToString()}")

        val id = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerModsChanged — unexpected id type: ${it.contentToString()}")
            return@Listener
        }
        val mods = it[1] as? JSONArray ?: run {
            Multiplayer.log("WARNING: playerModsChanged — unexpected mods type: ${it.contentToString()}")
            return@Listener
        }

        playerEventListener?.onPlayerModsChange(id, RoomMods(mods))
    }

    private val roomModsChanged = Listener {
        Multiplayer.log("RECEIVED: roomModsChanged -> ${it.contentToString()}")

        val mods = it[0] as? JSONArray ?: run {
            Multiplayer.log("WARNING: roomModsChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomModsChange(RoomMods(mods))
    }

    private val roomGameplaySettingsChanged = Listener {
        Multiplayer.log("RECEIVED: roomGameplaySettingsChanged -> ${it.contentToString()}")

        val json = it[0] as? JSONObject ?: run {
            Multiplayer.log("WARNING: roomGameplaySettingsChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomGameplaySettingsChange(parseGameplaySettings(json))
    }

    private val playerStatusChanged = Listener {
        //Multiplayer.log("RECEIVED: playerStatusChanged -> ${it.contentToString()}")

        val id = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unexpected id type: ${it.contentToString()}")
            return@Listener
        }
        val statusString = it[1] as? String ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unexpected status type: ${it.contentToString()}")
            return@Listener
        }
        val status = PlayerStatus.fromWire(statusString) ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unknown PlayerStatus value $statusString, ignoring event")
            return@Listener
        }

        playerEventListener?.onPlayerStatusChange(id, status)
    }

    private val teamModeChanged = Listener {
        Multiplayer.log("RECEIVED: teamModeChanged -> ${it.contentToString()}")

        val value = it[0] as? String ?: run {
            Multiplayer.log("WARNING: teamModeChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val mode = TeamMode.fromWire(value) ?: run {
            Multiplayer.log("WARNING: teamModeChanged — unknown TeamMode value $value, ignoring event")
            return@Listener
        }
        roomEventListener?.onRoomTeamModeChange(mode)
    }

    private val winConditionChanged = Listener {
        Multiplayer.log("RECEIVED: winConditionChanged -> ${it.contentToString()}")

        val value = it[0] as? String ?: run {
            Multiplayer.log("WARNING: winConditionChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val condition = WinCondition.fromWire(value) ?: run {
            Multiplayer.log("WARNING: winConditionChanged — unknown WinCondition value $value, ignoring event")
            return@Listener
        }
        roomEventListener?.onRoomWinConditionChange(condition)
    }

    private val teamChanged = Listener {
        Multiplayer.log("RECEIVED: teamChanged -> ${it.contentToString()}")

        val id = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: teamChanged — unexpected id type: ${it.contentToString()}")
            return@Listener
        }
        val team = when {
            it[1] == null -> null
            it[1] is String -> {
                val s = it[1] as String
                RoomTeam.fromWire(s) ?: run {
                    Multiplayer.log("WARNING: teamChanged — unknown RoomTeam value $s, ignoring event")
                    return@Listener
                }
            }
            else -> {
                Multiplayer.log("WARNING: teamChanged — unexpected team type: ${it.contentToString()}")
                return@Listener
            }
        }

        playerEventListener?.onPlayerTeamChange(id, team)
    }

    private val roomNameChanged = Listener {

        Multiplayer.log("RECEIVED: roomNameChanged -> ${it.contentToString()}")
        val name = it[0] as? String ?: run {
            Multiplayer.log("WARNING: roomNameChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomNameChange(name)
    }

    private val maxPlayersChanged = Listener {

        Multiplayer.log("RECEIVED: maxPlayersChanged -> ${it.contentToString()}")
        val maxPlayers = (it[0] as? String)?.toIntOrNull() ?: run {
            Multiplayer.log("WARNING: maxPlayersChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomMaxPlayersChange(maxPlayers)
    }

    private val playBeatmap = Listener {

        Multiplayer.log("RECEIVED: playBeatmap -> ${it.contentToString()}")

        val roundId = (it.getOrNull(0) as? Number)?.toInt() ?: run {
            Multiplayer.log("WARNING: playBeatmap — unexpected payload: ${it.contentToString()}")
            return@Listener
        }

        roomEventListener?.onRoomMatchPlay(roundId)
    }

    private val chatMessage = Listener {

        //Multiplayer.log("RECEIVED: chatMessage -> ${it.contentToString()}")

        val message = it[1] as? String ?: run {
            Multiplayer.log("WARNING: chatMessage — unexpected message type: ${it.contentToString()}")
            return@Listener
        }

        // Private replies from the server to a player have no ID.
        val id = (it.getOrNull(2) as? Number)?.toInt()

        if (id != null && !Multiplayer.session.acceptChat(id)) {
            Multiplayer.log("INFO: chatMessage — dropped already seen message $id")
            return@Listener
        }

        roomEventListener?.onRoomChatMessage((it[0] as? String)?.toLongOrNull(), message)
    }

    private val liveScoreData = Listener {

        val json = it[0] as JSONArray

        //Multiplayer.log("RECEIVED: liveScoreData -> ${it.contentToString()}")
        roomEventListener?.onRoomLiveLeaderboard(json)
    }

    // Server-to-client events

    private fun createInitialConnectionListener(expectedSocket: Socket) = Listener {
        if (socket !== expectedSocket) {
            return@Listener
        }

        val json = it[0] as? JSONObject ?: run {
            Multiplayer.log("WARNING: initialConnection — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }

        Multiplayer.log("RECEIVED: initialConnection\n${json.toString(3)}")

        try {
            val players = parsePlayers(json.getJSONArray("players"), json.getInt("maxPlayers"))
            val activePlayers = players.filterNotNull()

            val room = Room(
                id = json.getString("id").toLong(),
                name = json.getString("name"),
                isLocked = json.getBoolean("isLocked"),
                maxPlayers = json.getInt("maxPlayers"),
                mods = RoomMods(json.getJSONArray("mods")),
                gameplaySettings = parseGameplaySettings(json.getJSONObject("gameplaySettings")),
                teamMode = TeamMode.fromWire(json.getString("teamMode")) ?: TeamMode.HeadToHead,
                winCondition = WinCondition.fromWire(json.getString("winCondition")) ?: WinCondition.ScoreV1,
                playerCount = activePlayers.size,
                playerNames = activePlayers.joinToString(separator = ", ") { p -> p.name },
                sessionID = json.getString("sessionId")
            )

            room.players = players
            room.host = json.getJSONObject("host").getString("id").toLong()
            room.beatmap = parseBeatmap(json.optJSONObject("beatmap"))
            room.status = RoomStatus.fromWire(json.getString("status"))

            val localPlayer = room.playersMap[OnlineManager.getInstance().userId]

            if (localPlayer == null) {
                Multiplayer.log("ERROR: initialConnection — local player UID not found in server player list. Disconnecting.")
                roomEventListener?.onRoomConnectFail("Server did not include local player in room state.")

                socket = null

                expectedSocket.off()
                expectedSocket.disconnect()
                return@Listener
            }

            val recovery = json.toMultiplayerRecoveryPayload(localPlayer.status)
            val isReconnecting = Multiplayer.isReconnecting

            // This handler runs on the socket's event thread, so no chat message can be received between resetting the
            // session and seeding it.
            if (!isReconnecting) {
                Multiplayer.session.reset()
                Multiplayer.session.seedChat(recovery.latestChatId)
            }

            expectedSocket.apply {
                on(MultiplayerServerEvent.BeatmapChanged.eventName, beatmapChanged)
                on(MultiplayerServerEvent.HostChanged.eventName, hostChanged)
                on(MultiplayerServerEvent.PlayerKicked.eventName, playerKicked)
                on(MultiplayerServerEvent.PlayerModsChanged.eventName, playerModsChanged)
                on(MultiplayerServerEvent.RoomModsChanged.eventName, roomModsChanged)
                on(MultiplayerServerEvent.RoomGameplaySettingsChanged.eventName, roomGameplaySettingsChanged)
                on(MultiplayerServerEvent.PlayerStatusChanged.eventName, playerStatusChanged)
                on(MultiplayerServerEvent.TeamModeChanged.eventName, teamModeChanged)
                on(MultiplayerServerEvent.WinConditionChanged.eventName, winConditionChanged)
                on(MultiplayerServerEvent.TeamChanged.eventName, teamChanged)
                on(MultiplayerServerEvent.RoomNameChanged.eventName, roomNameChanged)
                on(MultiplayerServerEvent.MaxPlayersChanged.eventName, maxPlayersChanged)
                on(MultiplayerServerEvent.PlayBeatmap.eventName, playBeatmap)
                on(MultiplayerServerEvent.ChatMessage.eventName, chatMessage)
                on(MultiplayerServerEvent.LiveScoreData.eventName, liveScoreData)
                on(MultiplayerServerEvent.PlayerJoined.eventName, playerJoined)
                on(MultiplayerServerEvent.PlayerLeft.eventName, playerLeft)
                on(MultiplayerServerEvent.AllPlayersBeatmapLoadComplete.eventName, allPlayersBeatmapLoadComplete)
                on(MultiplayerServerEvent.AllPlayersSkipRequested.eventName, allPlayersSkipRequested)
                on(MultiplayerServerEvent.AllPlayersScoreSubmitted.eventName, allPlayersScoreSubmitted)
            }

            // During reconnection, reuse the RoomScene. Creating a new one would:
            // - replace roomEventListener / playerEventListener with a scene that is never displayed (see
            //   onRoomConnect for more details; essentially, its show() method is never called during reconnection), and
            // - leave the on-screen scene as a dead UI shell that can never receive events.
            // Instead, update the existing scene's room reference in-place and re-register it as the active listener,
            // then forward the connect event to it.
            val existingScene = if (isReconnecting) Multiplayer.roomScene else null

            val scene = if (existingScene != null) {
                existingScene.room = room

                // Re-register the existing scene as the event listener in case something
                // inadvertently replaced it while the reconnection was in flight.
                playerEventListener = existingScene
                roomEventListener = existingScene

                existingScene
            } else RoomScene(room)

            Multiplayer.room = room
            Multiplayer.player = localPlayer
            Multiplayer.roomScene = scene

            if (isReconnecting) {
                // Replay the messages missed while disconnected through the same path as live messages. They are
                // dropped if they were already seen.
                for (message in recovery.recentChat) {
                    if (Multiplayer.session.acceptChat(message.id)) {
                        scene.onRoomChatMessage(
                            if (message is MultiplayerMessage.Player) message.uid else null,
                            message.message
                        )
                    }
                }
            }

            transport.establishedSocket = expectedSocket

            // onRoomConnect triggers UI mutations (updateInformation, updatePlayerList, show, etc.), so we move its
            // operations to the update thread.
            updateThread { scene.onRoomConnect(room, recovery, isReconnecting) }
        } catch (e: Exception) {
            Multiplayer.log("ERROR: initialConnection handler threw an exception: ${e.javaClass.simpleName} — ${e.message}")
            Multiplayer.log(e)

            roomEventListener?.onRoomConnectFail("Unexpected error processing server room state: ${e.message}")

            socket = null

            expectedSocket.off()
            expectedSocket.disconnect()
        }
    }

    private val playerJoined = Listener {
        Multiplayer.log("RECEIVED: playerJoined -> ${it.contentToString()}")

        val json = it[0] as? JSONObject ?: run {
            Multiplayer.log("WARNING: playerJoined — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val player = parsePlayer(json)

        playerEventListener?.onPlayerJoin(player)
    }

    private val playerLeft = Listener {

        Multiplayer.log("RECEIVED: playerLeft -> ${it.contentToString()}")
        val uid = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerLeft — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        playerEventListener?.onPlayerLeft(uid)
    }

    private val allPlayersBeatmapLoadComplete = Listener {

        Multiplayer.log("RECEIVED: allPlayersBeatmapLoadComplete")
        roomEventListener?.onRoomMatchStart()
    }

    private val allPlayersSkipRequested = Listener {

        Multiplayer.log("RECEIVED: allPlayersSkipRequested")
        roomEventListener?.onRoomMatchSkip()
    }

    private val allPlayersScoreSubmitted = Listener {

        val array = it[0] as? JSONArray ?: run {
            Multiplayer.log("WARNING: allPlayersScoreSubmitted — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }

        val roundId = (it.getOrNull(1) as? Number)?.toInt() ?: run {
            Multiplayer.log("WARNING: allPlayersScoreSubmitted — unexpected round ID: ${it.contentToString()}")
            return@Listener
        }

        Multiplayer.log("RECEIVED: allPlayersScoreSubmitted\n${array.toString(3)}")
        roomEventListener?.onRoomFinalLeaderboard(array, roundId)
    }

    private fun createErrorListener(expectedSocket: Socket) = Listener {
        if (socket !== expectedSocket) return@Listener

        Multiplayer.log("RECEIVED: error -> ${it.contentToString()}")

        val error = it[0] as? String ?: run {
            Multiplayer.log("WARNING: error — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onServerError(error)
    }

    // Default listeners

    private fun createConnectErrorListener(expectedSocket: Socket) = Listener {
        if (socket !== expectedSocket) {
            return@Listener
        }

        Multiplayer.log("RECEIVED: connect_error -> ${it.contentToString()}")

        transport.establishedSocket = null

        roomEventListener?.onRoomConnectFail(it[0].toString())

        socket = null

        expectedSocket.off()
        expectedSocket.disconnect()
    }

    private fun createDisconnectListener(expectedSocket: Socket) = Listener {
        if (socket !== expectedSocket) return@Listener

        Multiplayer.log("RECEIVED: disconnect -> ${it.contentToString()}")

        // Cleared first so that acknowledgement timeouts racing this disconnect hold their events instead of resending.
        transport.establishedSocket = null
        Multiplayer.session.outbox.onDisconnected()

        val reason = it.getOrNull(0) as? String

        roomEventListener?.onRoomDisconnect(
            reason = reason,
            // Socket was manually disconnected by either server or client.
            byUser = reason == "io server disconnect" || reason == "io client disconnect"
        )

        socket = null

        expectedSocket.off()
        expectedSocket.disconnect()
    }


    // Emitters

    /**
     * Connect to the specified room, if success it'll call [IRoomEventListener.onRoomConnect] if not
     * [IRoomEventListener.onRoomConnectFail].
     *
     * @param lastChatId The ID of the latest chat message the client already has, if any. The server
     * sends the missed messages back in `initialConnection`.
     */
    fun connectToRoom(roomId: Long, userId: Long, gameSessionId: String, roomPassword: String? = null,
                      multiplayerSessionID: String? = null, lastChatId: Int? = null) {
        transport.establishedSocket = null
        lastRoomPassword = roomPassword

        val oldSocket = socket
        socket = null

        oldSocket?.off()
        oldSocket?.disconnect()

        val url = "${LobbyAPI.HOST}/$roomId"
        val auth = mutableMapOf<String, String>()

        // Blank passwords are not sent, so they must not be signed either.
        val roomPassword = roomPassword?.takeUnless { it.isBlank() }

        val sign = MultiplayerAttestation.signPayload(
            arrayOf<Any>(
                roomId,
                userId,
                gameSessionId,
                multiplayerSessionID ?: "",
                roomPassword ?: "",
                API_VERSION,
                "connect"
            ).joinToString("|")
        )

        auth["uid"] = userId.toString()
        auth["gameSessionID"] = gameSessionId
        auth["version"] = API_VERSION.toString()
        auth["sign"] = sign

        if (multiplayerSessionID != null) {
            auth["multiplayerSessionID"] = multiplayerSessionID
        }

        if (roomPassword != null) {
            auth["password"] = roomPassword
        }

        // Not part of the signed payload - just so that the server knows
        // the last chat ID that the client receives.
        if (lastChatId != null) {
            auth["lastChatId"] = lastChatId.toString()
        }

        Multiplayer.log("Starting connection -> $roomId, $userId")

        val newSocket = if (BuildSettings.MOCK_MULTIPLAYER) MockSocket(userId) else IO.socket(url, IO.Options().also {
            it.auth = auth

            // Explicitly not allow the socket to reconnect as we are using our own
            // reconnection system (the socket.io Java client does not support connection
            // state recovery).
            it.reconnection = false
        })

        socket = newSocket

        newSocket.apply {

            on(MultiplayerServerEvent.InitialConnection.eventName, createInitialConnectionListener(this))
            on(MultiplayerServerEvent.Error.eventName, createErrorListener(this))

            on(Socket.EVENT_CONNECT_ERROR, createConnectErrorListener(this))
            on(Socket.EVENT_DISCONNECT, createDisconnectListener(this))

        }.connect()
    }

    /**
     * Disconnect from socket.
     */
    fun disconnect() {
        transport.establishedSocket = null

        socket?.apply {
            Multiplayer.log("Disconnected from socket.")
            off()
            disconnect()
        }

        socket = null
    }

    // Host only

    /**
     * Change room beatmap.
     */
    @JvmOverloads
    @JvmStatic
    fun changeBeatmap(md5: String? = null, title: String? = null, artist: String? = null, version: String? = null, creator: String? = null) {

        val json = JSONObject().apply {

            put("md5", md5)
            put("title", title)
            put("artist", artist)
            put("version", version)
            put("creator", creator)

        }

        sendOnce(MultiplayerClientEvent.BeatmapChanged, json)
        Multiplayer.log("EMITTED: beatmapChanged -> $md5, $title, $artist, $version, $creator")
    }

    /**
     * Kick player from room.
     */
    fun kickPlayer(uid: Long) {
        sendOnce(MultiplayerClientEvent.PlayerKicked, uid.toString())
        Multiplayer.log("EMITTED: playerKicked -> $uid")
    }

    /**
     * Notify all clients to start loading beatmap.
     */
    fun notifyMatchPlay() {
        sendOnce(MultiplayerClientEvent.PlayBeatmap)
        Multiplayer.log("EMITTED: playBeatmap")
    }

    /**
     * Change room host.
     */
    fun setRoomHost(uid: Long) {
        sendOnce(MultiplayerClientEvent.HostChanged, uid.toString())
        Multiplayer.log("EMITTED: hostChanged -> $uid")
    }

    /**
     * Change room mods.
     */
    @JvmStatic
    fun setRoomMods(mods: String) {
        sendOnce(MultiplayerClientEvent.RoomModsChanged, JSONArray(mods))
        Multiplayer.log("EMITTED: roomModsChanged -> $mods")
    }

    /**
     * Change the remove slider lock setting.
     */
    fun setRoomRemoveSliderLock(isEnabled: Boolean) {
        val json = JSONObject().apply {
            put("isRemoveSliderLock", isEnabled)
        }

        sendOnce(MultiplayerClientEvent.RoomGameplaySettingsChanged, json)
        Multiplayer.log("EMITTED: roomGameplaySettingsChanged -> $json")
    }

    /**
     * Change `free mods` condition.
     */
    fun setRoomFreeMods(value: Boolean) {
        val json = JSONObject().apply {
            put("isFreeMod", value)
        }

        sendOnce(MultiplayerClientEvent.RoomGameplaySettingsChanged, json)
        Multiplayer.log("EMITTED: roomGameplaySettingsChanged -> $json")
    }

    /**
     * Change room team mode.
     */
    fun setRoomTeamMode(mode: TeamMode) {
        sendOnce(MultiplayerClientEvent.TeamModeChanged, mode.name)
        Multiplayer.log("EMITTED: teamModeChanged -> $mode")
    }

    /**
     * Change room win condition.
     */
    fun setRoomWinCondition(condition: WinCondition) {
        sendOnce(MultiplayerClientEvent.WinConditionChanged, condition.name)
        Multiplayer.log("EMITTED: winConditionChanged -> $condition")
    }

    /**
     * Change room name.
     */
    fun setRoomName(name: String) {
        sendOnce(MultiplayerClientEvent.RoomNameChanged, name)
        Multiplayer.log("EMITTED: roomNameChanged -> $name")
    }

    /**
     * Change room max players.
     */
    fun setRoomMaxPlayers(maxPlayers: Int) {
        sendOnce(MultiplayerClientEvent.MaxPlayersChanged, maxPlayers)
        Multiplayer.log("EMITTED: maxPlayersChanged -> $maxPlayers")
    }

    /**
     * Change room password.
     */
    fun setRoomPassword(password: String) {
        sendOnce(MultiplayerClientEvent.RoomPasswordChanged, password)
        //Multiplayer.log("EMITTED: roomPasswordChanged -> $password")
    }

    // All players

    /**
     * Submit the match score at the end of the game. The score is resent after a reconnection until the server
     * acknowledges it, as long as the round it belongs to is still the current one.
     */
    @JvmStatic
    fun submitFinalScore(json: JSONObject?) {
        val session = Multiplayer.session

        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, session.activeRoundId, arrayOf(json))
        Multiplayer.log("EMITTED: scoreSubmission\n${json?.toString(2)}")
    }

    /**
     * Submit the live score of the player during the game.
     */
    @JvmStatic
    fun submitLiveScore(json: JSONObject?) {
        socket?.emit(MultiplayerClientEvent.LiveScoreData.eventName, json) ?: run {
            //Multiplayer.log("WARNING: Tried to emit event 'liveScoreData' while socket is null.")
            return
        }

        // We don't indent here to avoid spam
        Multiplayer.log("EMITTED: liveScoreData -> $json")
    }

    /**
     * Notify beatmap finish load. The notification is resent after a reconnection until the server acknowledges it, as
     * long as the round is still the current one.
     *
     * @param roundId The ID of the round the beatmap was loaded for.
     */
    @JvmOverloads
    fun notifyBeatmapLoaded(roundId: Int? = Multiplayer.session.activeRoundId) {
        Multiplayer.session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.BeatmapLoadComplete, roundId, arrayOf())
        Multiplayer.log("EMITTED: beatmapLoadComplete")
    }

    /**
     * Request skip. The request is resent after a reconnection until the server acknowledges it, as long as the round
     * is still the current one.
     */
    fun requestSkip() {
        val session = Multiplayer.session

        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.SkipRequested, session.activeRoundId, arrayOf())
        Multiplayer.log("EMITTED: skipRequested")
    }

    /**
     * Send chat message. Messages are resent after a reconnection until the server acknowledges them, except for chat
     * commands (starting with `!`), which are not resent.
     */
    fun sendMessage(message: String) {
        // Chat commands (such as "!start") are never resent, since running one long after the user sent it could act
        // on a room that has since changed.
        if (message.trimStart().startsWith("!")) {
            sendOnce(MultiplayerClientEvent.ChatMessage, message)
            return
        }

        // The server drops a message whose ID it has already seen, so resending it cannot show it twice.
        Multiplayer.session.outbox.sendUntilAcknowledged(
            MultiplayerClientEvent.ChatMessage, null, arrayOf(message, UUID.randomUUID().toString())
        )
        //Multiplayer.log("EMITTED: chatMessage -> $message")
    }

    /**
     * Change player status.
     */
    @JvmStatic
    fun setPlayerStatus(status: PlayerStatus) {
        sendOnce(MultiplayerClientEvent.PlayerStatusChanged, status.name)
        Multiplayer.log("EMITTED: playerStatusChanged -> $status")
    }

    /**
     * Change player mods.
     */
    @JvmStatic
    fun setPlayerMods(mods: String) {
        sendOnce(MultiplayerClientEvent.PlayerModsChanged, JSONArray(mods))
        Multiplayer.log("EMITTED: playerModsChanged -> $mods")
    }

    /**
     * Change player team.
     */
    fun setPlayerTeam(team: RoomTeam) {
        sendOnce(MultiplayerClientEvent.TeamChanged, team.name)
        Multiplayer.log("EMITTED: teamChanged -> $team")
    }

    /**
     * Sends an event that is not resent after a reconnection.
     *
     * [IRoomEventListener.onEventNotDelivered] is called if the server does not acknowledge it.
     */
    private fun sendOnce(event: MultiplayerClientEvent, vararg args: Any?) {
        Multiplayer.session.outbox.sendOnce(event, arrayOf(*args)) { whileConnected ->
            roomEventListener?.onEventNotDelivered(event, whileConnected)
        }
    }
}