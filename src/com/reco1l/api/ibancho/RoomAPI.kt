package com.reco1l.api.ibancho

import com.reco1l.api.ibancho.data.*
import com.reco1l.api.ibancho.data.Room
import com.reco1l.api.ibancho.data.RoomTeam
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logE
import com.reco1l.framework.extensions.logI
import com.reco1l.framework.extensions.logIfDebug
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.emitter.Emitter.Listener
import org.json.JSONArray
import org.json.JSONObject
import kotlin.Exception

object RoomAPI
{

    /**
     * The API version.
     */
    const val API_VERSION = 1

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

        val json = it[0] as? JSONObject
        val beatmap = parseBeatmap(json)

        roomEventListener?.onRoomBeatmapChange(beatmap)
    }

    private val hostChanged = Listener {

        roomEventListener?.onRoomHostChange((it[0] as String).toLong())
    }

    private val playerKicked = Listener {

        playerEventListener?.onPlayerKick((it[0] as String).toLong())
    }

    private val playerModsChanged = Listener {

        val id = (it[0] as String).toLong()
        val mods = it[1] as String?

        playerEventListener?.onPlayerModsChange(id, mods)
    }

    private val roomModsChanged = Listener {

        val mods = it[0] as String?

        roomEventListener?.onRoomModsChange(mods)
    }

    private val freeModsSettingChanged = Listener {

        roomEventListener?.onRoomFreeModsChange(it[0] as Boolean)
    }

    private val playerStatusChanged = Listener {

        val id = (it[0] as String).toLong()
        val status = PlayerStatus.from(it[1] as Int)

        playerEventListener?.onPlayerStatusChange(id, status)
    }

    private val teamModeChanged = Listener {

        val mode = TeamMode.from(it[0] as Int)

        roomEventListener?.onRoomTeamModeChange(mode)
    }

    private val winConditionChanged = Listener {

        val condition = WinCondition.from(it[0] as Int)

        roomEventListener?.onRoomWinConditionChange(condition)
    }

    private val teamChanged = Listener {

        val id = (it[0] as String).toLong()
        val team = if (it[1] == null) null else RoomTeam.from(it[1] as Int)

        playerEventListener?.onPlayerTeamChange(id, team)
    }

    private val roomNameChanged = Listener {

        roomEventListener?.onRoomNameChange(it[0] as String)
    }

    private val playBeatmap = Listener {

        roomEventListener?.onRoomMatchPlay()
    }

    private val chatMessage = Listener {

        roomEventListener?.onRoomChatMessage(it[0] as? String, it[1] as String)
    }

    private val liveScoreData = Listener {

        val json = it[0] as JSONArray

        roomEventListener?.onRoomLiveLeaderboard(json)
    }

    // Server-to-client events

    private val initialConnection = Listener {

        val json = it[0] as JSONObject

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

        val json = it[0] as JSONObject
        val player = parsePlayer(json)

        playerEventListener?.onPlayerJoin(player)
    }

    private val playerLeft = Listener {

        playerEventListener?.onPlayerLeft((it[0] as String).toLong())
    }

    private val allPlayersBeatmapLoadComplete = Listener { roomEventListener?.onRoomMatchStart() }

    private val allPlayersSkipRequested = Listener { roomEventListener?.onRoomMatchSkip() }

    private val allPlayersScoreSubmitted = Listener {

        val array = it[0] as JSONArray

        roomEventListener?.onRoomFinalLeaderboard(array)
    }

    private val error = Listener {

        val error = it[0] as String

        error.logE(className)
        roomEventListener?.onServerError(error)
    }

    // Default listeners

    private val onConnectionError = Listener {

        socket?.off()
        socket = null
        roomEventListener?.onRoomConnectFail(it[0] as Exception)

    }

    private val onDisconnect = Listener {

        socket?.off()
        socket = null
        "Disconnected from socket".logI(className)

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
        "Connecting to: $url".logIfDebug(className)

        val auth = mutableMapOf<String, String>()

        auth["uid"] = userId.toString()
        auth["username"] = username
        auth["version"] = API_VERSION.toString()

        if (!roomPassword.isNullOrBlank())
            auth["password"] = roomPassword

        val options = IO.Options()
        options.auth = auth

        socket = IO.socket(url, options).apply {

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

            on(Socket.EVENT_CONNECT) {
                "Connected successfully".logI(className)
            }
            on(Socket.EVENT_CONNECT_ERROR, onConnectionError)
            on(Socket.EVENT_DISCONNECT, onDisconnect)

        }.connect()
    }

    /**
     * Disconnect from socket.
     */
    fun disconnect()
    {
        socket?.apply {

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
    }

    /**
     * Kick player from room.
     */
    fun kickPlayer(uid: Long): Emitter = socket!!.emit("playerKicked", uid.toString())

    /**
     * Notify all clients to start loading beatmap.
     */
    fun notifyMatchPlay(): Emitter = socket!!.emit("playBeatmap")

    /**
     * Change room host.
     */
    fun setRoomHost(uid: Long): Emitter = socket!!.emit("hostChanged", uid.toString())

    /**
     * Change room mods.
     */
    fun setRoomMods(mods: String?): Emitter = socket!!.emit("roomModsChanged", mods)

    /**
     * Change `free mods` condition.
     */
    fun setRoomFreeMods(value: Boolean): Emitter = socket!!.emit("freeModsSettingChanged", value)

    /**
     * Change room team mode.
     */
    fun setRoomTeamMode(mode: TeamMode): Emitter = socket!!.emit("teamModeChanged", mode.ordinal)

    /**
     * Change room win condition.
     */
    fun setRoomWinCondition(condition: WinCondition): Emitter = socket!!.emit("winConditionChanged", condition.ordinal)

    /**
     * Change room name.
     */
    fun setRoomName(name: String): Emitter = socket!!.emit("roomNameChanged", name)

    /**
     * Change room password.
     */
    fun setRoomPassword(password: String): Emitter = socket!!.emit("roomPasswordChanged", password)

    // All players

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitFinalScore(json: JSONObject?): Emitter = socket!!.emit("scoreSubmission", json)

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitLiveScore(json: JSONObject?): Emitter = socket!!.emit("liveScoreData", json)

    /**
     * Notify beatmap finish load.
     */
    fun notifyBeatmapLoaded(): Emitter = socket!!.emit("beatmapLoadComplete")

    /**
     * Request skip.
     */
    fun requestSkip(): Emitter = socket!!.emit("skipRequested")

    /**
     * Send chat message.
     */
    fun sendMessage(message: String): Emitter = socket!!.emit("chatMessage", message)

    /**
     * Change player status.
     */
    @JvmStatic
    fun setPlayerStatus(status: PlayerStatus): Emitter = socket!!.emit("playerStatusChanged", status.ordinal)

    /**
     * Change player mods.
     */
    fun setPlayerMods(mods: String?): Emitter = socket!!.emit("playerModsChanged", mods)

    /**
     * Change player team.
     */
    fun setPlayerTeam(team: RoomTeam): Emitter = socket!!.emit("teamChanged", team.ordinal)

}