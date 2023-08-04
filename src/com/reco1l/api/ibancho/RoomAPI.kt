package com.reco1l.api.ibancho

import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.api.ibancho.data.*
import com.reco1l.api.ibancho.data.Room
import com.reco1l.api.ibancho.data.RoomTeam
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logI
import com.reco1l.legacy.ui.multiplayer.multiLog
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter.Listener
import org.json.JSONArray
import org.json.JSONObject

object RoomAPI
{

    /**
     * The API version.
     */
    private const val API_VERSION = 1

    /**
     * The listener for player events.
     */
    var playerEventListener: IPlayerEventListener? = null

    /**
     * The listener for room events.
     */
    var roomEventListener: IRoomEventListener? = null


    private var socket: Socket? = null
        set(value)
        {
            field = value
            multiLog("Socket set to $value")
        }


    // https://gist.github.com/Rian8337/ceab4d3b179cbeee7dd548cfcf145b95
    // Back-to-back events

    private val beatmapChanged = Listener {
        multiLog("RECEIVED: beatmapChanged -> ${it.contentToString()}")

        val json = it[0] as? JSONObject
        val beatmap = parseBeatmap(json)

        roomEventListener?.onRoomBeatmapChange(beatmap)
    }

    private val hostChanged = Listener {

        multiLog("RECEIVED: hostChanged -> ${it.contentToString()}")
        roomEventListener?.onRoomHostChange((it[0] as String).toLong())
    }

    private val playerKicked = Listener {

        multiLog("RECEIVED: playerKicked -> ${it.contentToString()}")
        playerEventListener?.onPlayerKick((it[0] as String).toLong())
    }

    private val playerModsChanged = Listener {
        multiLog("RECEIVED: playerModsChanged -> ${it.contentToString()}")

        val id = (it[0] as String).toLong()
        val mods = it[1] as String?

        playerEventListener?.onPlayerModsChange(id, mods)
    }

    private val roomModsChanged = Listener {
        multiLog("RECEIVED: roomModsChanged -> ${it.contentToString()}")

        val mods = it[0] as String?
        roomEventListener?.onRoomModsChange(mods)
    }

    private val freeModsSettingChanged = Listener {
        multiLog("RECEIVED: freeModsSettingChanged -> ${it.contentToString()}")

        roomEventListener?.onRoomFreeModsChange(it[0] as Boolean)
    }

    private val playerStatusChanged = Listener {
        //multiLog("RECEIVED: playerStatusChanged -> ${it.contentToString()}")

        val id = (it[0] as String).toLong()
        val status = PlayerStatus.from(it[1] as Int)

        playerEventListener?.onPlayerStatusChange(id, status)
    }

    private val teamModeChanged = Listener {
        multiLog("RECEIVED: teamModeChanged -> ${it.contentToString()}")

        val mode = TeamMode.from(it[0] as Int)
        roomEventListener?.onRoomTeamModeChange(mode)
    }

    private val winConditionChanged = Listener {
        multiLog("RECEIVED: winConditionChanged -> ${it.contentToString()}")

        val condition = WinCondition.from(it[0] as Int)
        roomEventListener?.onRoomWinConditionChange(condition)
    }

    private val teamChanged = Listener {
        multiLog("RECEIVED: teamChanged -> ${it.contentToString()}")

        val id = (it[0] as String).toLong()
        val team = if (it[1] == null) null else RoomTeam.from(it[1] as Int)

        playerEventListener?.onPlayerTeamChange(id, team)
    }

    private val roomNameChanged = Listener {

        multiLog("RECEIVED: roomNameChanged -> ${it.contentToString()}")
        roomEventListener?.onRoomNameChange(it[0] as String)
    }

    private val playBeatmap = Listener {

        multiLog("RECEIVED: playBeatmap -> ${it.contentToString()}")
        roomEventListener?.onRoomMatchPlay()
    }

    private val chatMessage = Listener {

        //multiLog("RECEIVED: chatMessage -> ${it.contentToString()}")
        roomEventListener?.onRoomChatMessage(it[0] as? String, it[1] as String)
    }

    private val liveScoreData = Listener {

        val json = it[0] as JSONArray

        //multiLog("RECEIVED: liveScoreData -> ${it.contentToString()}")
        roomEventListener?.onRoomLiveLeaderboard(json)
    }

    // Server-to-client events

    private val initialConnection = Listener {

        val json = it[0] as JSONObject

        multiLog("RECEIVED: initialConnection\n${json.toString(3)}")

        val players = parsePlayers(json.getJSONArray("players"), json.getInt("maxPlayers"))
        val activePlayers = players.filterNotNull()

        val room = Room(
                id = json.getString("id").toLong(),
                name = json.getString("name"),
                isLocked = json.getBoolean("isLocked"),
                maxPlayers = json.getInt("maxPlayers"),
                mods = json.getString("mods"),
                isFreeMods = json.getBoolean("isFreeMod"),
                teamMode = TeamMode.from(json.getInt("teamMode")),
                winCondition = WinCondition.from(json.getInt("winCondition")),
                playerCount = activePlayers.size,
                playerNames = activePlayers.joinToString(separator = ", ") { p -> p.name }
        ).apply {

            this.players = players
            host = json.getJSONObject("host").getString("uid").toLong()
            beatmap = parseBeatmap(json.optJSONObject("beatmap"))
            status = RoomStatus.from(json.getInt("status"))

        }
        roomEventListener?.onRoomConnect(room)
    }

    private val playerJoined = Listener {
        multiLog("RECEIVED: playerJoined -> ${it.contentToString()}")

        val json = it[0] as JSONObject
        val player = parsePlayer(json)

        playerEventListener?.onPlayerJoin(player)
    }

    private val playerLeft = Listener {

        multiLog("RECEIVED: playerLeft -> ${it.contentToString()}")
        playerEventListener?.onPlayerLeft((it[0] as String).toLong())
    }

    private val allPlayersBeatmapLoadComplete = Listener {

        multiLog("RECEIVED: allPlayersBeatmapLoadComplete")
        roomEventListener?.onRoomMatchStart()
    }

    private val allPlayersSkipRequested = Listener {

        multiLog("RECEIVED: allPlayersSkipRequested")
        roomEventListener?.onRoomMatchSkip()
    }

    private val allPlayersScoreSubmitted = Listener {

        val array = it[0] as JSONArray

        multiLog("RECEIVED: allPlayersScoreSubmitted\n${array.toString(3)}")
        roomEventListener?.onRoomFinalLeaderboard(array)
    }

    private val error = Listener {
        multiLog("RECEIVED: error -> ${it.contentToString()}")

        val error = it[0] as String
        roomEventListener?.onServerError(error)
    }

    // Default listeners

    private val onConnectionError = Listener {
        multiLog("RECEIVED: onConnectionError -> ${it.contentToString()}")

        socket?.off()
        socket = null

        roomEventListener?.onRoomConnectFail(it[0].toString())
    }

    private val onDisconnect = Listener {
        multiLog("RECEIVED: onDisconnect -> ${it.contentToString()}")

        socket?.off()
        socket = null

        roomEventListener?.onRoomDisconnect()
    }

    // Emitters

    /**
     * Connect to the specified room, if success it'll call [IRoomEventListener.onRoomConnect] if not
     * [IRoomEventListener.onRoomConnectFail]
     */
    fun connectToRoom(roomId: Long, userId: Long, username: String, roomPassword: String?)
    {
        if (socket != null)
            throw IllegalStateException("Cannot connect to another room socket while there's already connected.")

        val url = "${LobbyAPI.HOST}/$roomId"
        val auth = mutableMapOf<String, String>()
        val sign = SecurityUtils.signRequest("${userId}_$username")

        auth["uid"] = userId.toString()
        auth["username"] = username
        auth["version"] = API_VERSION.toString()

        if (sign != null)
            auth["authSign"] = sign

        if (!roomPassword.isNullOrBlank())
            auth["password"] = roomPassword

        multiLog("Starting connection -> $roomId, $userId, $username")

        socket = IO.socket(url, IO.Options().also { it.auth = auth }).apply {

            on("beatmapChanged", beatmapChanged)
            on("hostChanged", hostChanged)
            on("playerKicked", playerKicked)
            on("playerModsChanged", playerModsChanged)
            on("roomModsChanged", roomModsChanged)
            on("freeModsSettingChanged", freeModsSettingChanged)
            on("playerStatusChanged", playerStatusChanged)
            on("teamModeChanged", teamModeChanged)
            on("winConditionChanged", winConditionChanged)
            on("teamChanged", teamChanged)
            on("roomNameChanged", roomNameChanged)
            on("playBeatmap", playBeatmap)
            on("chatMessage", chatMessage)
            on("liveScoreData", liveScoreData)

            on("initialConnection", initialConnection)
            on("playerJoined", playerJoined)
            on("playerLeft", playerLeft)
            on("allPlayersBeatmapLoadComplete", allPlayersBeatmapLoadComplete)
            on("allPlayersSkipRequested", allPlayersSkipRequested)
            on("allPlayersScoreSubmitted", allPlayersScoreSubmitted)
            on("error", error)

            on(Socket.EVENT_CONNECT_ERROR, onConnectionError)
            on(Socket.EVENT_DISCONNECT, onDisconnect)

        }.connect()
    }

    /**
     * Disconnect from socket.
     */
    fun disconnect()
    {
        if (socket == null)
            multiLog("WARNING: Tried to disconnect from a null socket.")

        socket?.apply {

            multiLog("Disconnected from socket.")
            disconnect()
            off()
        }
        socket = null
    }

    // Host only

    /**
     * Change room beatmap.
     */
    @JvmOverloads
    fun changeBeatmap(md5: String? = null, title: String? = null, artist: String? = null, version: String? = null, creator: String? = null)
    {
        val json = JSONObject().apply {

            put("md5", md5)
            put("title", title)
            put("artist", artist)
            put("version", version)
            put("creator", creator)

        }

        socket!!.emit("beatmapChanged", json)
        multiLog("EMITTED: beatmapChanged -> $md5, $title, $artist, $version, $creator")
    }

    /**
     * Kick player from room.
     */
    fun kickPlayer(uid: Long)
    {
        socket!!.emit("playerKicked", uid.toString())
        multiLog("EMITTED: playerKicked -> $uid")
    }

    /**
     * Notify all clients to start loading beatmap.
     */
    fun notifyMatchPlay()
    {
        socket!!.emit("playBeatmap")
        multiLog("EMITTED: playBeatmap")
    }

    /**
     * Change room host.
     */
    fun setRoomHost(uid: Long)
    {
        socket!!.emit("hostChanged", uid.toString())
        multiLog("EMITTED: hostChanged -> $uid")
    }

    /**
     * Change room mods.
     */
    @JvmStatic
    fun setRoomMods(mods: String?)
    {
        socket!!.emit("roomModsChanged", mods)
        multiLog("EMITTED: roomModsChanged -> $mods")
    }

    /**
     * Change `free mods` condition.
     */
    fun setRoomFreeMods(value: Boolean)
    {
        socket!!.emit("freeModsSettingChanged", value)
        multiLog("EMITTED: freeModsSettingChanged -> $value")
    }

    /**
     * Change room team mode.
     */
    fun setRoomTeamMode(mode: TeamMode)
    {
        socket!!.emit("teamModeChanged", mode.ordinal)
        multiLog("EMITTED: teamModeChanged -> $mode")
    }

    /**
     * Change room win condition.
     */
    fun setRoomWinCondition(condition: WinCondition)
    {
        socket!!.emit("winConditionChanged", condition.ordinal)
        multiLog("EMITTED: winConditionChanged -> $condition")
    }

    /**
     * Change room name.
     */
    fun setRoomName(name: String)
    {
        socket!!.emit("roomNameChanged", name)
        multiLog("EMITTED: roomNameChanged -> $name")
    }

    /**
     * Change room password.
     */
    fun setRoomPassword(password: String)
    {
        socket!!.emit("roomPasswordChanged", password)
        //multiLog("EMITTED: roomPasswordChanged -> $password")
    }

    // All players

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitFinalScore(json: JSONObject?)
    {
        socket!!.emit("scoreSubmission", json)
        multiLog("EMITTED: scoreSubmission\n${json?.toString(2)}")
    }

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitLiveScore(json: JSONObject?)
    {
        socket!!.emit("liveScoreData", json)

        // We don't indent here to avoid spam
        multiLog("EMITTED: liveScoreData -> $json")
    }

    /**
     * Notify beatmap finish load.
     */
    fun notifyBeatmapLoaded()
    {
        socket!!.emit("beatmapLoadComplete")
        multiLog("EMITTED: beatmapLoadComplete")
    }

    /**
     * Request skip.
     */
    fun requestSkip()
    {
        socket!!.emit("skipRequested")
        multiLog("EMITTED: skipRequested")
    }

    /**
     * Send chat message.
     */
    fun sendMessage(message: String)
    {
        socket!!.emit("chatMessage", message)
        //multiLog("EMITTED: chatMessage -> $message")
    }

    /**
     * Change player status.
     */
    @JvmStatic
    fun setPlayerStatus(status: PlayerStatus)
    {
        socket!!.emit("playerStatusChanged", status.ordinal)
        multiLog("EMITTED: playerStatusChanged -> $status")
    }

    /**
     * Change player mods.
     */
    @JvmStatic
    fun setPlayerMods(mods: String?)
    {
        socket!!.emit("playerModsChanged", mods)
        multiLog("EMITTED: playerModsChanged -> $mods")
    }

    /**
     * Change player team.
     */
    fun setPlayerTeam(team: RoomTeam)
    {
        socket!!.emit("teamChanged", team.ordinal)
        multiLog("EMITTED: teamChanged -> $team")
    }

}