package com.osudroid.multiplayer.api

import com.osudroid.BuildSettings
import com.osudroid.debug.MockSocket
import com.osudroid.multiplayer.*
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
import com.osudroid.ui.v2.multi.RoomScene
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter.Listener
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object RoomAPI {

    /**
     * The API version.
     */
    const val API_VERSION = 9


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
        val statusOrdinal = it[1] as? Int ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unexpected status type: ${it.contentToString()}")
            return@Listener
        }
        val status = PlayerStatus[statusOrdinal] ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unknown PlayerStatus ordinal $statusOrdinal, ignoring event")
            return@Listener
        }

        playerEventListener?.onPlayerStatusChange(id, status)
    }

    private val teamModeChanged = Listener {
        Multiplayer.log("RECEIVED: teamModeChanged -> ${it.contentToString()}")

        val ordinal = it[0] as? Int ?: run {
            Multiplayer.log("WARNING: teamModeChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val mode = TeamMode[ordinal] ?: run {
            Multiplayer.log("WARNING: teamModeChanged — unknown TeamMode ordinal $ordinal, ignoring event")
            return@Listener
        }
        roomEventListener?.onRoomTeamModeChange(mode)
    }

    private val winConditionChanged = Listener {
        Multiplayer.log("RECEIVED: winConditionChanged -> ${it.contentToString()}")

        val ordinal = it[0] as? Int ?: run {
            Multiplayer.log("WARNING: winConditionChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val condition = WinCondition.from(ordinal) ?: run {
            Multiplayer.log("WARNING: winConditionChanged — unknown WinCondition ordinal $ordinal, ignoring event")
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
            it[1] is Int  -> {
                val n = it[1] as Int
                RoomTeam[n] ?: run {
                    Multiplayer.log("WARNING: teamChanged — unknown RoomTeam ordinal $n, ignoring event")
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
        roomEventListener?.onRoomMatchPlay()
    }

    private val chatMessage = Listener {

        //Multiplayer.log("RECEIVED: chatMessage -> ${it.contentToString()}")

        val message = it[1] as? String ?: run {
            Multiplayer.log("WARNING: chatMessage — unexpected message type: ${it.contentToString()}")
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

    private val initialConnection = Listener {

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
                teamMode = TeamMode[json.getInt("teamMode")] ?: TeamMode.HeadToHead,
                winCondition = WinCondition.from(json.getInt("winCondition")) ?: WinCondition.ScoreV1,
                playerCount = activePlayers.size,
                playerNames = activePlayers.joinToString(separator = ", ") { p -> p.name },
                sessionID = json.getString("sessionId")
            )

            room.players = players
            room.host = json.getJSONObject("host").getString("id").toLong()
            room.beatmap = parseBeatmap(json.optJSONObject("beatmap"))
            room.status = RoomStatus[json.getInt("status")]

            // Locate our own player entry before registering further listeners.
            // If the server sends a player list that does not include our UID (malformed
            // payload or API mismatch) we cannot safely proceed — disconnect and surface
            // an error rather than crashing with NullPointerException.
            val localPlayer = room.playersMap[OnlineManager.getInstance().userId]
            if (localPlayer == null) {
                Multiplayer.log("ERROR: initialConnection — local player UID not found in server player list. Disconnecting.")
                roomEventListener?.onRoomConnectFail("Server did not include local player in room state.")
                val s = socket
                socket = null
                s?.off()
                s?.disconnect()
                return@Listener
            }

            socket?.apply {
                on("beatmapChanged", beatmapChanged)
                on("hostChanged", hostChanged)
                on("playerKicked", playerKicked)
                on("playerModsChanged", playerModsChanged)
                on("roomModsChanged", roomModsChanged)
                on("roomGameplaySettingsChanged", roomGameplaySettingsChanged)
                on("playerStatusChanged", playerStatusChanged)
                on("teamModeChanged", teamModeChanged)
                on("winConditionChanged", winConditionChanged)
                on("teamChanged", teamChanged)
                on("roomNameChanged", roomNameChanged)
                on("maxPlayersChanged", maxPlayersChanged)
                on("playBeatmap", playBeatmap)
                on("chatMessage", chatMessage)
                on("liveScoreData", liveScoreData)
                on("playerJoined", playerJoined)
                on("playerLeft", playerLeft)
                on("allPlayersBeatmapLoadComplete", allPlayersBeatmapLoadComplete)
                on("allPlayersSkipRequested", allPlayersSkipRequested)
                on("allPlayersScoreSubmitted", allPlayersScoreSubmitted)
            }

            // During reconnection we must NOT create a new RoomScene.  Creating one would:
            //   • replace RoomAPI.roomEventListener / playerEventListener with a scene that
            //     is never displayed (SI-1 — the ghost-scene bug), and
            //   • leave the on-screen scene as a dead UI shell that can never receive events.
            // Instead, update the existing scene's room reference in-place and re-register it
            // as the active listener, then forward the connect event to it.
            val existingScene = if (Multiplayer.isReconnecting) Multiplayer.roomScene else null

            val scene = if (existingScene != null) {
                // Reuse the displayed scene.  Update its room reference so all subsequent reads
                // of `this.room` inside event handlers see the fresh server state.
                existingScene.room = room
                // Re-register the existing scene as the event listener in case something
                // inadvertently replaced it while the reconnection was in flight.
                playerEventListener = existingScene
                roomEventListener = existingScene
                existingScene
            } else {
                // Fresh connection: build and register a brand-new scene as normal.
                RoomScene(room)
            }

            Multiplayer.room = room
            Multiplayer.player = localPlayer
            Multiplayer.roomScene = scene

            scene.onRoomConnect(room)

        } catch (e: Exception) {
            Multiplayer.log("ERROR: initialConnection handler threw an exception: ${e.javaClass.simpleName} — ${e.message}")
            Multiplayer.log(e)
            roomEventListener?.onRoomConnectFail("Unexpected error processing server room state: ${e.message}")
            val s = socket
            socket = null
            s?.off()
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

        Multiplayer.log("RECEIVED: allPlayersScoreSubmitted\n${array.toString(3)}")
        roomEventListener?.onRoomFinalLeaderboard(array)
    }

    private val error = Listener {
        Multiplayer.log("RECEIVED: error -> ${it.contentToString()}")

        val error = it[0] as? String ?: run {
            Multiplayer.log("WARNING: error — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onServerError(error)
    }

    // Default listeners

    private val connectError = Listener {
        Multiplayer.log("RECEIVED: connect_error -> ${it.contentToString()}")

        roomEventListener?.onRoomConnectFail(it[0].toString())

        // Capture the reference before nulling so a concurrent connectToRoom() that has
        // already assigned a new socket is not accidentally cleared.
        val s = socket
        socket = null
        s?.off()
    }

    private val disconnect = Listener {
        Multiplayer.log("RECEIVED: disconnect -> ${it.contentToString()}")

        val reason = it.getOrNull(0) as? String

        roomEventListener?.onRoomDisconnect(
            reason = reason,
            // Socket was manually disconnected by either server or client.
            byUser = reason == "io server disconnect" || reason == "io client disconnect"
        )
    }


    // Emitters

    /**
     * Connect to the specified room, if success it'll call [IRoomEventListener.onRoomConnect] if not
     * [IRoomEventListener.onRoomConnectFail]
     */
    fun connectToRoom(roomId: Long, userId: Long, gameSessionId: String, roomPassword: String? = null,
                      multiplayerSessionID: String? = null) {

        // Capture and clear the old socket reference BEFORE creating the new one.
        // This means any late callbacks still firing on the old socket will read null
        // from the field and cannot accidentally wipe out the new socket reference.
        val oldSocket = socket
        socket = null
        oldSocket?.off()

        val url = "${LobbyAPI.HOST}/$roomId"
        val auth = mutableMapOf<String, String>()
        val sign = SecurityUtils.signRequest("${userId}_$gameSessionId")

        auth["uid"] = userId.toString()
        auth["gameSessionID"] = gameSessionId
        auth["version"] = API_VERSION.toString()

        if (multiplayerSessionID != null) {
            auth["multiplayerSessionID"] = multiplayerSessionID
        }

        if (sign != null) {
            auth["authSign"] = sign
        }

        if (!roomPassword.isNullOrBlank()) {
            auth["password"] = roomPassword
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

            on("initialConnection", initialConnection)
            on("error", error)

            on(Socket.EVENT_CONNECT_ERROR, connectError)
            on(Socket.EVENT_DISCONNECT, disconnect)

        }.connect()
    }

    /**
     * Disconnect from socket.
     */
    fun disconnect() {

        if (socket == null) {
            return
        }

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

        socket?.emit("beatmapChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'beatmapChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: beatmapChanged -> $md5, $title, $artist, $version, $creator")
    }

    /**
     * Kick player from room.
     */
    fun kickPlayer(uid: Long) {
        socket?.emit("playerKicked", uid.toString()) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playerKicked' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: playerKicked -> $uid")
    }

    /**
     * Notify all clients to start loading beatmap.
     */
    fun notifyMatchPlay() {
        socket?.emit("playBeatmap") ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playBeatmap' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: playBeatmap")
    }

    /**
     * Change room host.
     */
    fun setRoomHost(uid: Long) {
        socket?.emit("hostChanged", uid.toString()) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'hostChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: hostChanged -> $uid")
    }

    /**
     * Change room mods.
     */
    @JvmStatic
    fun setRoomMods(mods: String) {
        socket?.emit("roomModsChanged", JSONArray(mods)) ?: run {
			Multiplayer.log("WARNING: Tried to emit event 'roomModsChanged' while socket is null.")
			return
		}
        Multiplayer.log("EMITTED: roomModsChanged -> $mods")
    }

    /**
     * Change the remove slider lock setting.
     */
    fun setRoomRemoveSliderLock(isEnabled: Boolean) {
        val json = JSONObject().apply {
            put("isRemoveSliderLock", isEnabled)
        }

        socket?.emit("roomGameplaySettingsChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomGameplaySettingsChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomGameplaySettingsChanged -> $json")
    }

    /**
     * Change `free mods` condition.
     */
    fun setRoomFreeMods(value: Boolean) {
        val json = JSONObject().apply {
            put("isFreeMod", value)
        }

        socket?.emit("roomGameplaySettingsChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomGameplaySettingsChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomGameplaySettingsChanged -> $json")
    }

    /**
     * Change room team mode.
     */
    fun setRoomTeamMode(mode: TeamMode) {
        socket?.emit("teamModeChanged", mode.ordinal) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'teamModeChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: teamModeChanged -> $mode")
    }

    /**
     * Change room win condition.
     */
    fun setRoomWinCondition(condition: WinCondition) {
        socket?.emit("winConditionChanged", condition.ordinal) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'winConditionChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: winConditionChanged -> $condition")
    }

    /**
     * Change room name.
     */
    fun setRoomName(name: String) {
        socket?.emit("roomNameChanged", name) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomNameChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomNameChanged -> $name")
    }

    /**
     * Change room max players.
     */
    fun setRoomMaxPlayers(maxPlayers: Int) {
        socket?.emit("maxPlayersChanged", maxPlayers) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'maxPlayersChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: maxPlayersChanged -> $maxPlayers")
    }

    /**
     * Change room password.
     */
    fun setRoomPassword(password: String) {
        socket?.emit("roomPasswordChanged", password) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomPasswordChanged' while socket is null.")
            return
        }
        //Multiplayer.log("EMITTED: roomPasswordChanged -> $password")
    }

    // All players

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitFinalScore(json: JSONObject?) {
        socket?.emit("scoreSubmission", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'scoreSubmission' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: scoreSubmission\n${json?.toString(2)}")
    }

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitLiveScore(json: JSONObject?) {
        socket?.emit("liveScoreData", json) ?: run {
            //Multiplayer.log("WARNING: Tried to emit event 'liveScoreData' while socket is null.")
            return
        }

        // We don't indent here to avoid spam
        Multiplayer.log("EMITTED: liveScoreData -> $json")
    }

    /**
     * Notify beatmap finish load.
     */
    fun notifyBeatmapLoaded() {
        socket?.emit("beatmapLoadComplete") ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'beatmapLoadComplete' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: beatmapLoadComplete")
    }

    /**
     * Request skip.
     */
    fun requestSkip() {
        socket?.emit("skipRequested") ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'skipRequested' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: skipRequested")
    }

    /**
     * Send chat message.
     */
    fun sendMessage(message: String) {
        socket?.emit("chatMessage", message) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'chatMessage' while socket is null.")
            return
        }
        //Multiplayer.log("EMITTED: chatMessage -> $message")
    }

    /**
     * Change player status.
     */
    @JvmStatic
    fun setPlayerStatus(status: PlayerStatus) {
        socket?.emit("playerStatusChanged", status.ordinal) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playerStatusChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: playerStatusChanged -> $status")
    }

    /**
     * Change player mods.
     */
    @JvmStatic
    fun setPlayerMods(mods: String) {
        socket?.emit("playerModsChanged", JSONArray(mods)) ?: run {
			Multiplayer.log("WARNING: Tried to emit event 'playerModsChanged' while socket is null.")
			return
		}

        Multiplayer.log("EMITTED: playerModsChanged -> $mods")
    }

    /**
     * Change player team.
     */
    fun setPlayerTeam(team: RoomTeam) {
        socket?.emit("teamChanged", team.ordinal) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'teamChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: teamChanged -> $team")
    }

}