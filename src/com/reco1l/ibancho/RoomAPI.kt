package com.reco1l.ibancho

import ru.nsu.ccfit.zuev.osu.SecurityUtils
import com.reco1l.ibancho.data.PlayerStatus
import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomStatus
import com.reco1l.ibancho.data.RoomTeam
import com.reco1l.ibancho.data.TeamMode
import com.reco1l.ibancho.data.WinCondition
import com.reco1l.ibancho.data.parseBeatmap
import com.reco1l.ibancho.data.parseGameplaySettings
import com.reco1l.ibancho.data.parseMods
import com.reco1l.ibancho.data.parsePlayer
import com.reco1l.ibancho.data.parsePlayers
import com.reco1l.osu.multiplayer.Multiplayer
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter.Listener
import org.json.JSONArray
import org.json.JSONObject

object RoomAPI {

    /**
     * The API version.
     */
    private const val API_VERSION = 7


    /**
     * The listener for player events.
     */
    var playerEventListener: IPlayerEventListener? = null

    /**
     * The listener for room events.
     */
    var roomEventListener: IRoomEventListener? = null


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
        roomEventListener?.onRoomHostChange((it[0] as String).toLong())
    }

    private val playerKicked = Listener {

        Multiplayer.log("RECEIVED: playerKicked -> ${it.contentToString()}")
        playerEventListener?.onPlayerKick((it[0] as String).toLong())
    }

    private val playerModsChanged = Listener {
        Multiplayer.log("RECEIVED: playerModsChanged -> ${it.contentToString()}")

        val id = (it[0] as String).toLong()
        val json = it[1] as JSONObject

        playerEventListener?.onPlayerModsChange(id, parseMods(json))
    }

    private val roomModsChanged = Listener {
        Multiplayer.log("RECEIVED: roomModsChanged -> ${it.contentToString()}")

        val json = it[0] as JSONObject
        roomEventListener?.onRoomModsChange(parseMods(json))
    }

    private val roomGameplaySettingsChanged = Listener {
        Multiplayer.log("RECEIVED: roomGameplaySettingsChanged -> ${it.contentToString()}")

        val json = it[0] as JSONObject
        roomEventListener?.onRoomGameplaySettingsChange(parseGameplaySettings(json))
    }

    private val playerStatusChanged = Listener {
        //Multiplayer.log("RECEIVED: playerStatusChanged -> ${it.contentToString()}")

        val id = (it[0] as String).toLong()
        val status = PlayerStatus[it[1] as Int]

        playerEventListener?.onPlayerStatusChange(id, status)
    }

    private val teamModeChanged = Listener {
        Multiplayer.log("RECEIVED: teamModeChanged -> ${it.contentToString()}")

        val mode = TeamMode[it[0] as Int]
        roomEventListener?.onRoomTeamModeChange(mode)
    }

    private val winConditionChanged = Listener {
        Multiplayer.log("RECEIVED: winConditionChanged -> ${it.contentToString()}")

        val condition = WinCondition.from(it[0] as Int)
        roomEventListener?.onRoomWinConditionChange(condition)
    }

    private val teamChanged = Listener {
        Multiplayer.log("RECEIVED: teamChanged -> ${it.contentToString()}")

        val id = (it[0] as String).toLong()
        val team = if (it[1] == null) null else RoomTeam[it[1] as Int]

        playerEventListener?.onPlayerTeamChange(id, team)
    }

    private val roomNameChanged = Listener {

        Multiplayer.log("RECEIVED: roomNameChanged -> ${it.contentToString()}")
        roomEventListener?.onRoomNameChange(it[0] as String)
    }

    private val playBeatmap = Listener {

        Multiplayer.log("RECEIVED: playBeatmap -> ${it.contentToString()}")
        roomEventListener?.onRoomMatchPlay()
    }

    private val chatMessage = Listener {

        //Multiplayer.log("RECEIVED: chatMessage -> ${it.contentToString()}")

        roomEventListener?.onRoomChatMessage((it[0] as? String)?.toLongOrNull(), it[1] as String)
    }

    private val liveScoreData = Listener {

        val json = it[0] as JSONArray

        //Multiplayer.log("RECEIVED: liveScoreData -> ${it.contentToString()}")
        roomEventListener?.onRoomLiveLeaderboard(json)
    }

    // Server-to-client events

    private val initialConnection = Listener {

        val json = it[0] as JSONObject

        Multiplayer.log("RECEIVED: initialConnection\n${json.toString(3)}")

        val players = parsePlayers(json.getJSONArray("players"), json.getInt("maxPlayers"))
        val activePlayers = players.filterNotNull()

        val room = Room(
            id = json.getString("id").toLong(),
            name = json.getString("name"),
            isLocked = json.getBoolean("isLocked"),
            maxPlayers = json.getInt("maxPlayers"),
            mods = parseMods(json.getJSONObject("mods")),
            gameplaySettings = parseGameplaySettings(json.getJSONObject("gameplaySettings")),
            teamMode = TeamMode[json.getInt("teamMode")],
            winCondition = WinCondition.from(json.getInt("winCondition")),
            playerCount = activePlayers.size,
            playerNames = activePlayers.joinToString(separator = ", ") { p -> p.name },
            sessionID = json.getString("sessionId")
        )

        room.players = players
        room.host = json.getJSONObject("host").getString("uid").toLong()
        room.beatmap = parseBeatmap(json.optJSONObject("beatmap"))
        room.status = RoomStatus[json.getInt("status")]


        roomEventListener?.onRoomConnect(room)

        socket!!.apply {
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
            on("playBeatmap", playBeatmap)
            on("chatMessage", chatMessage)
            on("liveScoreData", liveScoreData)
            on("playerJoined", playerJoined)
            on("playerLeft", playerLeft)
            on("allPlayersBeatmapLoadComplete", allPlayersBeatmapLoadComplete)
            on("allPlayersSkipRequested", allPlayersSkipRequested)
            on("allPlayersScoreSubmitted", allPlayersScoreSubmitted)
        }
    }

    private val playerJoined = Listener {
        Multiplayer.log("RECEIVED: playerJoined -> ${it.contentToString()}")

        val json = it[0] as JSONObject
        val player = parsePlayer(json)

        playerEventListener?.onPlayerJoin(player)
    }

    private val playerLeft = Listener {

        Multiplayer.log("RECEIVED: playerLeft -> ${it.contentToString()}")
        playerEventListener?.onPlayerLeft((it[0] as String).toLong())
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

        val array = it[0] as JSONArray

        Multiplayer.log("RECEIVED: allPlayersScoreSubmitted\n${array.toString(3)}")
        roomEventListener?.onRoomFinalLeaderboard(array)
    }

    private val error = Listener {
        Multiplayer.log("RECEIVED: error -> ${it.contentToString()}")

        val error = it[0] as String
        roomEventListener?.onServerError(error)
    }

    // Default listeners

    private val connectError = Listener {
        Multiplayer.log("RECEIVED: connect_error -> ${it.contentToString()}")

        roomEventListener?.onRoomConnectFail(it[0].toString())

        socket?.off()
        socket = null
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
    fun connectToRoom(roomId: Long, userId: Long, username: String, roomPassword: String? = null, sessionID: String? = null) {

        // Clearing previous socket in case of reconnection.
        socket?.off()
        socket = null

        val url = "${LobbyAPI.HOST}/$roomId"
        val auth = mutableMapOf<String, String>()
        val sign = SecurityUtils.signRequest("${userId}_$username")

        auth["uid"] = userId.toString()
        auth["username"] = username
        auth["version"] = API_VERSION.toString()

        if (sessionID != null) {
            auth["sessionID"] = sessionID
        }

        if (sign != null) {
            auth["authSign"] = sign
        }

        if (!roomPassword.isNullOrBlank()) {
            auth["password"] = roomPassword
        }

        Multiplayer.log("Starting connection -> $roomId, $userId, $username")

        socket = IO.socket(url, IO.Options().also {
            it.auth = auth

            // Explicitly not allow the socket to reconnect as we are using our own
            // reconnection system (the socket.io Java client does not support connection
            // state recovery).
            it.reconnection = false
        }).apply {

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
        socket!!.emit("playBeatmap")
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
    fun setRoomMods(
        mods: String?,
        speedMultiplier: Float,
        flFollowDelay: Float,
        customAR: Float? = null,
        customOD: Float? = null,
        customCS: Float? = null,
        customHP: Float? = null
    ) {
        val json = JSONObject().apply {

            put("mods", mods)
            put("speedMultiplier", speedMultiplier)
            put("flFollowDelay", flFollowDelay)
            put("customAR", customAR)
            put("customOD", customOD)
            put("customCS", customCS)
            put("customHP", customHP)

        }
        socket?.emit("roomModsChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomModsChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomModsChanged -> $json")
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
     * Change `allow force difficulty statistics` condition.
     */
    fun setRoomAllowForceDifficultyStatistics(value: Boolean) {
        val json = JSONObject().apply {
            put("allowForceDifficultyStatistics", value)
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
        socket!!.emit("beatmapLoadComplete")
        Multiplayer.log("EMITTED: beatmapLoadComplete")
    }

    /**
     * Request skip.
     */
    fun requestSkip() {
        socket!!.emit("skipRequested")
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
    @JvmOverloads
    fun setPlayerMods(
        mods: String?,
        speedMultiplier: Float,
        flFollowDelay: Float,
        customAR: Float? = null,
        customOD: Float? = null,
        customCS: Float? = null,
        customHP: Float? = null
    ) {
        val json = JSONObject().apply {

            put("mods", mods)
            put("speedMultiplier", speedMultiplier)
            put("flFollowDelay", flFollowDelay)

            put("customAR", customAR)
            put("customOD", customOD)
            put("customCS", customCS)
            put("customHP", customHP)
        }
        socket?.emit("playerModsChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playerModsChanged' while socket is null.")
            return
        }

        Multiplayer.log("EMITTED: playerModsChanged -> $json")
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