package com.reco1l.legacy.ui.multiplayer

import android.app.AlertDialog
import com.reco1l.api.ibancho.IPlayerEventListener
import com.reco1l.api.ibancho.IRoomEventListener
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.*
import com.reco1l.api.ibancho.data.PlayerStatus.*
import com.reco1l.api.ibancho.data.TeamMode.HEAD_TO_HEAD
import com.reco1l.api.ibancho.data.TeamMode.TEAM_VS_TEAM
import com.reco1l.api.ibancho.data.WinCondition.*
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.orCatch
import com.reco1l.framework.lang.glThread
import com.reco1l.framework.lang.uiThread
import com.reco1l.legacy.data.modsToReadable
import com.reco1l.legacy.data.modsToString
import com.reco1l.legacy.data.stringToMods
import com.reco1l.legacy.ui.entity.BeatmapButton
import com.reco1l.legacy.ui.entity.ComposedText
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.MOD_SCOREV2
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen.LoadingScene
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.LibraryManager.INSTANCE as library
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.menu.ModMenu.getInstance as getModMenu
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object RoomScene : Scene(), IRoomEventListener, IPlayerEventListener
{

    /**
     * The current room, it can become `null` if socket disconnects.
     */
    @JvmStatic
    var room: Room? = null
        set(value)
        {
            Multiplayer.isConnected = value != null

            if (value == null)
            {
                Multiplayer.isRoomHost = false
                player = null
            }
            field = value
        }

    /**
     * The player that correspond us according to client UID, it can become `null` if socket disconnects.
     */
    @JvmStatic
    var player: RoomPlayer? = null

    /**
     * Indicates that the host can change beatmap (it should be false while a change request was done)
     *
     * This is only used if [player] is the room host.
     */
    var awaitBeatmapChange = false

    /**
     * Indicates that the player can change its status, its purpose is to await server changes.
     */
    @JvmField
    var awaitStatusChange = false

    /**
     * Indicates that the player can change its mods, its purpose is to await server changes.
     */
    var awaitModsChange = false

    /**
     * Indicates that the player has the beatmap locally.
     */
    @JvmField
    var hasLocalTrack = false


    val chat = RoomChat()

    val chatPreview = ComposedText(0f, 0f, getResources().getFont("smallFont"), 100)

    val leaveDialog = AlertDialog.Builder(getGlobal().mainActivity).apply {

        setTitle("Leave room")
        setMessage("Are you sure?")
        setPositiveButton("Yes") { dialog, _ ->

            dialog.dismiss()
            back()
        }
        setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

    }


    private var backButton: Sprite? = null

    private var onlinePanel: OnlinePanel? = null

    private var readyButton: TextButton? = null

    private var secondaryButton: TextButton? = null

    private var trackButton: BeatmapButton? = null

    private var modsButton: AnimSprite? = null

    private var playerList: RoomPlayerList? = null

    private var options: RoomOptions? = null


    private val titleText = ChangeableText(20f, 20f, getResources().getFont("bigFont"), "", 100)

    private val stateText = ChangeableText(0f, 0f, getResources().getFont("smallFont"), "", 100)

    private val infoText = ChangeableText(0f, 0f, getResources().getFont("smallFont"), "", 100)


    fun load()
    {
        detachChildren()
        clearTouchAreas()

        isBackgroundEnabled = true

        // Background dim
        val dim = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        dim.setColor(0f, 0f, 0f, 0.5f)
        attachChild(dim, 0)

        // Top bar
        val top = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 120f)
        top.setColor(0f, 0f, 0f, 0.3f)
        attachChild(top)

        // Title
        attachChild(titleText)

        // State
        stateText.setPosition(20f, titleText.y + titleText.height)
        stateText.setColor(0.8f, 0.8f, 0.8f)
        attachChild(stateText)

        // Track selection button
        trackButton = BeatmapButton().also {

            it.setPosition(Config.getRES_WIDTH() - it.width + 20f, 130f + 40f)

            registerTouchArea(it)
            attachChild(it)
        }

        // Preview text
        chatPreview.setPosition(trackButton!!.x + 20f, 130f + 10f)
        attachChild(chatPreview)

        // Info text
        infoText.setPosition(trackButton!!.x + 20f, trackButton!!.y + trackButton!!.height + 10f)
        attachChild(infoText)

        // Ready button, this button will switch player status
        readyButton = object : TextButton(getResources().getFont("CaptionFont"), "Ready")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitStatusChange)
                    return false

                awaitStatusChange = true

                // Switching status
                when (player!!.status)
                {
                    NOT_READY ->
                    {
                        if (room!!.beatmap == null)
                        {
                            ToastLogger.showText("Cannot set ready when host is changing beatmap.", true)
                            awaitStatusChange = false
                            return true
                        }

                        if (room!!.teamMode == TEAM_VS_TEAM && player!!.team == null)
                        {
                            ToastLogger.showText("You need to select a team before to set ready.", true)
                            awaitStatusChange = false
                            return true
                        }
                        RoomAPI.setPlayerStatus(READY)
                    }

                    READY -> RoomAPI.setPlayerStatus(NOT_READY)
                    MISSING_BEATMAP ->
                    {
                        ToastLogger.showText("Beatmap is missing, cannot set ready.", true)
                        awaitStatusChange = false
                    }

                    else -> awaitStatusChange = false /*This case can never happen, the PLAYING status is set when a game starts*/
                }
                return true
            }
        }.also {

            it.width = 400f
            it.setColor(0.2f, 0.9f, 0.2f)
            it.setPosition(Config.getRES_WIDTH() - it.width - 20f, Config.getRES_HEIGHT() - it.height - 20f)

            registerTouchArea(it)
            attachChild(it)
        }

        // It'll only be shown if the player is the room host, if host status is set to READY, this button will start
        // the game otherwise it'll the options button.
        secondaryButton = object : TextButton(getResources().getFont("CaptionFont"), "Options")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitStatusChange)
                    return false

                if (player!!.status == READY)
                {
                    // Button shouldn't be visible at this point so ignoring input
                    if (!Multiplayer.isRoomHost)
                        return false

                    if (room!!.teamMode == HEAD_TO_HEAD)
                    {
                        // Checking if there's at least another player that isn't the host that has state READY.
                        if (room!!.activePlayers.any { it != player && it.status == READY })
                        {
                            RoomAPI.notifyMatchPlay()
                            return true
                        }
                        ToastLogger.showText("At least 2 players needs to be ready to start a match!", true)
                    }
                    else
                    {
                        val redTeam = room!!.redTeamPlayers
                        val blueTeam = room!!.blueTeamPlayers

                        if (redTeam.isEmpty() || blueTeam.isEmpty())
                        {
                            ToastLogger.showText("At least 1 player per team to start a match!", true)
                            return true
                        }

                        if (redTeam.any { it.status == READY } && blueTeam.any { it.status == READY })
                        {
                            RoomAPI.notifyMatchPlay()
                            return true
                        }
                        ToastLogger.showText("At least 1 player per team needs to be ready to start a match!", true)
                    }
                }
                else uiThread {
                    options = RoomOptions()
                    options!!.show()
                }
                return false
            }
        }.also {

            it.width = 400f
            it.setColor(0.2f, 0.2f, 0.2f)
            it.setPosition(Config.getRES_WIDTH() - it.width - 20f, readyButton!!.y - it.height - 20f)

            registerTouchArea(it)
            attachChild(it)
        }

        // Buttons code copied from legacy code but improved, don't blame on me.
        val layoutBackButton = OsuSkin.get().getLayout("BackButton")
        val layoutMods = OsuSkin.get().getLayout("ModsButton")

        val loadedBackTextures = mutableListOf<String>()

        if (getResources().isTextureLoaded("menu-back-0"))
        {
            for (i in 0..59)
                if (getResources().isTextureLoaded("menu-back-$i")) loadedBackTextures.add("menu-back-$i")
        }
        else loadedBackTextures.add("menu-back")

        backButton = object : AnimSprite(0f, 0f, loadedBackTextures.size.toFloat(), *loadedBackTextures.toTypedArray<String>())
        {
            var scaleWhenHold = layoutBackButton?.property?.optBoolean("scaleWhenHold", true) ?: false

            var moved = false
            var dx = 0f
            var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (event.isActionDown)
                {
                    if (scaleWhenHold) this.setScale(1.25f)

                    moved = false
                    dx = localX
                    dy = localY

                    getResources().getSound("menuback")?.play()
                    return true
                }
                if (event.isActionUp)
                {
                    this.setScale(1f)

                    if (!moved)
                        uiThread { leaveDialog.show() }
                    return true
                }
                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50)
                {
                    this.setScale(1f)
                    moved = true
                }
                return false
            }
        }.also {

            if (OsuSkin.get().isUseNewLayout)
            {
                layoutBackButton?.baseApply(it)
                it.setPosition(0f, Config.getRES_HEIGHT() - it.heightScaled)
            }
            else it.setPosition(0f, Config.getRES_HEIGHT() - it.height)

            registerTouchArea(it)
            attachChild(it)
        }

        // Mods button, shown only if free mods or if the player it's the host
        modsButton = object : AnimSprite(0f, 0f, 0f, "selection-mods", "selection-mods-over")
        {
            var moved = false
            var dx = 0f
            var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!Multiplayer.isRoomHost && !room!!.isFreeMods || awaitModsChange || player!!.status == READY)
                    return true

                if (event.isActionDown)
                {
                    moved = false
                    dx = localX
                    dy = localY

                    frame = 1
                    return true
                }
                if (event.isActionUp)
                {
                    frame = 0
                    if (!moved)
                        getModMenu().show(this@RoomScene, getGlobal().selectedTrack)
                    return true
                }
                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50)
                {
                    frame = 0
                    moved = true
                }
                return false
            }
        }.also {

            it.isVisible = false
            it.setScale(1.5f)

            registerTouchArea(it)
            attachChild(it)

            if (OsuSkin.get().isUseNewLayout)
            {
                layoutMods?.baseApply(it)
                it.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - it.heightScaled)
            }
            else it.setPosition(backButton!!.x + backButton!!.width, (Config.getRES_HEIGHT() - 90f))
        }

        // Online panel
        onlinePanel = OnlinePanel().also {

            it.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
            attachChild(it)
        }
    }


    // Update events

    private fun updateBackground(path: String?)
    {
        val texture = if (path != null) getResources().loadBackground(path) else getResources().getTexture("menu-background")

        val height = texture.height.toFloat() * (Config.getRES_WIDTH() / texture.width.toFloat())
        val menuBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, Config.getRES_WIDTH()
                .toFloat(), height, texture)

        background = SpriteBackground(menuBg)
    }

    private fun updateInformation()
    {
        infoText.text = """
            Team mode: ${
            when (room!!.teamMode)
            {
                HEAD_TO_HEAD -> "Head to Head"
                TEAM_VS_TEAM -> "Team vs Team"
            }
        } 
            Win condition: ${
            when (room!!.winCondition)
            {
                SCORE_V1 -> "Score V1"
                ACCURACY -> "Accuracy"
                MAX_COMBO -> "Max combo"
                SCORE_V2 -> "Score V2"
            }
        }
            Free mods: ${if (room!!.isFreeMods) "Yes" else "No"}
            Mods: ${modsToReadable(room!!.mods)}
        """.trimIndent()
    }


    // Clearing

    fun clear()
    {
        room = null

        // Clearing chat
        chat.log.clear()
        chat.dismiss()

        // Hide any player menu if its shown
        uiThread {
            playerList?.menu?.dismiss()
            options?.dismiss()
            Unit
        }

        // Clearing player list
        playerList?.detachSelf()
        playerList = null
    }


    // Navigation

    override fun back()
    {
        { RoomAPI.disconnect() }.orCatch { }
        clear()
        LobbyScene.show()
    }

    fun show()
    {
        if (Multiplayer.isConnected)
        {
            // Setting player status to NOT_READY
            awaitStatusChange = true

            if (hasLocalTrack || room!!.beatmap == null)
                RoomAPI.setPlayerStatus(NOT_READY)
            else
                RoomAPI.setPlayerStatus(MISSING_BEATMAP)
        }
        else
        {
            back()
            return
        }
        getGlobal().engine.scene = this
        chat.show()
    }


    // Communication

    override fun onServerError(error: String) = ToastLogger.showText(error, true)

    override fun onRoomChatMessage(username: String?, message: String)
    {
        // If username is null considering it as system message
        if (username != null)
        {
            val player = room!!.activePlayers.find { it.name == username } ?: return

            if (!player.isMuted)
                chat.onRoomChatMessage(username, message)
        }
        else
            chat.onSystemChatMessage(message, "#007BFF")
    }

    // Connection

    override fun onRoomConnect(newRoom: Room)
    {
        if (room != newRoom)
            chat.onSystemChatMessage("Welcome to osu!droid multiplayer", "#007BFF")

        // Setting new room
        room = newRoom

        // Releasing await locks just in case
        awaitModsChange = false
        awaitBeatmapChange = false
        awaitStatusChange = false

        // Reloading player list
        playerList?.detachSelf()
        playerList = RoomPlayerList(newRoom)
        attachChild(playerList, 1)

        // Reloading mod menu
        getModMenu().reload()

        // Finding our player object
        player = newRoom.getPlayerByUID(getOnline().userId) ?: run {

            ToastLogger.showText("Unable to find player", false)
            back()
            return
        }
        onPlayerStatusChange(player!!.id, player!!.status)

        // Applying all changes
        onRoomHostChange(newRoom.host)
        onRoomNameChange(newRoom.name)
        onRoomModsChange(newRoom.mods)
        onRoomBeatmapChange(newRoom.beatmap)
        onRoomFreeModsChange(newRoom.isFreeMods)

        playerList!!.updateItems()
        show()
    }

    override fun onRoomDisconnect()
    {
        room = null
        ToastLogger.showText("Disconnected from room", true)

        // If player is in one of these scenes we go back.
        if (getGlobal().engine.scene == this
                || getGlobal().engine.scene is LoadingScene
                || getGlobal().engine.scene == getGlobal().songMenu.scene)
            back()
    }

    override fun onRoomConnectFail(e: Exception)
    {
        room = null
        ToastLogger.showText("Failed to connect room: ${e.className} - ${e.message}", true)
        e.printStackTrace()
        back()
    }


    // Room changes

    override fun onRoomNameChange(name: String)
    {
        room!!.name = name
        titleText.text = name
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?)
    {
        // Setting await lock
        awaitStatusChange = true

        // Updating values
        room!!.beatmap = beatmap
        trackButton!!.beatmap = beatmap

        // If beatmap is `null` means no beatmap is selected
        if (beatmap == null)
        {
            awaitBeatmapChange = false
            updateBackground(null)
            RoomAPI.setPlayerStatus(NOT_READY)
            return
        }

        // Searching if the track is in our library
        val localTrack = library.findTrackByMD5(beatmap.md5)

        // Updating player status.
        if (localTrack == null)
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)
        else
            RoomAPI.setPlayerStatus(NOT_READY)

        // Updating track button.
        trackButton!!.loadTrack(localTrack)

        // Updating background
        updateBackground(localTrack?.background)

        // Releasing await lock
        awaitBeatmapChange = false

        // Playing selected track
        if (localTrack == null || localTrack == getGlobal().selectedTrack)
            return

        getGlobal().selectedTrack = localTrack

        // Preventing from change song when host is in room while other players are in gameplay
        if (getGlobal().engine.scene != getGlobal().gameScene.scene)
        {
            getGlobal().songService.preLoad(localTrack.beatmap.music)
            getGlobal().songService.play()
        }
    }

    override fun onRoomHostChange(uid: Long)
    {
        if (room!!.host != uid)
            chat.onSystemChatMessage("Player ${room!!.getPlayerByUID(uid)?.name} is now the room host.", "#007BFF")

        room!!.host = uid

        // Defining if is the host
        Multiplayer.isRoomHost = getOnline().userId == uid

        // Reloading mod menu
        glThread {
            clearChildScene()
            getModMenu().init()
        }

        // Hiding mod button in case isn't the host when free mods is disabled
        modsButton!!.isVisible = Multiplayer.isRoomHost || room!!.isFreeMods

        // Updating buttons visibility
        if (player!!.status == READY)
        {
            secondaryButton!!.isVisible = Multiplayer.isRoomHost

            if (Multiplayer.isRoomHost)
            {
                secondaryButton!!.setText("Start match!")
                secondaryButton!!.setColor(0.2f, 0.9f, 0.2f)
            }
        }
        else
        {
            secondaryButton!!.isVisible = true
            secondaryButton!!.setText("Options")
            secondaryButton!!.setColor(0.2f, 0.2f, 0.2f)
        }

        // Updating player list
        playerList!!.updateItems()
    }


    // Mods

    override fun onRoomModsChange(mods: String?)
    {
        room!!.mods = mods

        // Update room info text
        updateInformation()

        // If free mods is enabled it'll keep player mods and enforce speed changing mods and ScoreV2
        getModMenu().setMods(stringToMods(mods), room!!.isFreeMods)

        // Updating player mods
        awaitModsChange = true
        RoomAPI.setPlayerMods(modsToString(getModMenu().mod))

        // Setting player status to NOT_READY
        awaitStatusChange = true

        if (hasLocalTrack || room!!.beatmap == null)
            RoomAPI.setPlayerStatus(NOT_READY)
        else
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)
    }

    override fun onPlayerModsChange(uid: Long, mods: String?)
    {
        // Updating player mods
        room!!.getPlayerByUID(uid)!!.mods = mods

        // Updating player list
        playerList!!.updateItems()

        // Removing await lock
        if (uid == player!!.id)
            awaitModsChange = false
    }

    override fun onRoomFreeModsChange(isFreeMods: Boolean)
    {
        room!!.isFreeMods = isFreeMods
        // Update room info text
        updateInformation()

        // Closing mod menu, to enforce mod menu scene update
        clearChildScene()
        // Hiding mod button in case isn't the host when free mods is disabled
        modsButton!!.isVisible = Multiplayer.isRoomHost || room!!.isFreeMods

        // Updating mod set
        getModMenu().setMods(stringToMods(room!!.mods), room!!.isFreeMods)

        // Updating player mods
        awaitModsChange = true
        RoomAPI.setPlayerMods(room!!.mods)

        // Setting player status to NOT_READY
        awaitStatusChange = true

        if (hasLocalTrack || room!!.beatmap == null)
            RoomAPI.setPlayerStatus(NOT_READY)
        else
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)
    }

    override fun onRoomTeamModeChange(mode: TeamMode)
    {
        room!!.teamMode = mode

        // Update room info text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()

        // Setting player status to NOT_READY
        awaitStatusChange = true

        if (hasLocalTrack || room!!.beatmap == null)
            RoomAPI.setPlayerStatus(NOT_READY)
        else
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition)
    {
        room!!.winCondition = winCondition

        // Update room info text
        updateInformation()

        // If win condition is Score V2 we add the mod.
        val roomMods = stringToMods(room!!.mods)
        roomMods.remove(MOD_SCOREV2)

        if (winCondition == SCORE_V2)
            roomMods.add(MOD_SCOREV2)

        awaitModsChange = true

        // Applying to all room
        RoomAPI.setRoomMods(modsToString(roomMods))

        // Updating player list
        playerList!!.updateItems()

        // Setting player status to NOT_READY
        awaitStatusChange = true

        if (hasLocalTrack || room!!.beatmap == null)
            RoomAPI.setPlayerStatus(NOT_READY)
        else
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)
    }


    // Match

    override fun onRoomMatchPlay()
    {
        getGlobal().songMenu.stopMusic()

        Replay.oldMod = getModMenu().mod
        Replay.oldChangeSpeed = getModMenu().changeSpeed
        Replay.oldForceAR = getModMenu().forceAR
        Replay.oldEnableForceAR = getModMenu().isEnableForceAR
        Replay.oldFLFollowDelay = getModMenu().fLfollowDelay

        getGlobal().songMenu.game.startGame(getGlobal().selectedTrack, null)

        // Hiding any player menu if its shown
        uiThread { playerList!!.menu?.dismiss() }

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onRoomMatchStart()
    {
        RoomAPI.setPlayerStatus(PLAYING)
        getGlobal().gameScene.start()
    }

    override fun onRoomMatchSkip() = getGlobal().gameScene.skip()


    // Leaderboard

    override fun onRoomLiveLeaderboard(leaderboard: JSONArray) = Multiplayer.onLiveLeaderboard(leaderboard)

    override fun onRoomFinalLeaderboard(leaderboard: JSONArray) = Multiplayer.onFinalLeaderboard(leaderboard)


    // Player related events

    override fun onPlayerJoin(player: RoomPlayer)
    {
        var wasAlready = false

        // Determining if the player was already in the room (reconnect)
        val index = room!!.indexOfPlayer(player.id)?.also { wasAlready = true } ?: room!!.players.indexOfFirst { it == null }

        // Updating values
        room!!.players[index] = player

        if (!wasAlready)
            chat.onSystemChatMessage("Player ${player.name} joined.", "#007BFF")

        // Updating state text
        stateText.text = "${room!!.activePlayers.size} / ${room!!.maxPlayers} players. ${room!!.readyPlayers.size} players ready."

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerLeft(uid: Long)
    {
        val index = room!!.indexOfPlayer(uid)

        // Can never be the case
        if (index == null)
        {
            playerList!!.updateItems()
            return
        }

        // Notifying in chat
        room!!.getPlayerByUID(uid)?.also {
            chat.onSystemChatMessage("Player ${it.name} left.", "#007BFF")
        }

        // Updating values
        room!!.players[index] = null

        // Updating state text
        stateText.text = "${room!!.activePlayers.size} / ${room!!.maxPlayers} players. ${room!!.readyPlayers.size} players ready."

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerKick(uid: Long)
    {
        val index = room!!.indexOfPlayer(uid)

        // Can never be the case
        if (index == null)
        {
            playerList!!.updateItems()
            return
        }

        if (uid == player!!.id)
        {
            back()
            uiThread {
                AlertDialog.Builder(getGlobal().mainActivity).apply {

                    setTitle("Message")
                    setMessage("You've been kicked by room host.")
                    setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }

                }.show()
            }
            return
        }

        // Notifying in chat
        room!!.getPlayerByUID(uid)?.also {
            chat.onSystemChatMessage("Player ${it.name} was kicked.", "#FF0000")
        }

        // Updating values
        room!!.players[index] = null

        // Updating state text
        stateText.text = "${room!!.activePlayers.size} / ${room!!.maxPlayers} players. ${room!!.readyPlayers.size} players ready."

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus)
    {
        // Updating player status
        room!!.getPlayerByUID(uid)!!.status = status

        if (uid == player!!.id)
        {
            awaitStatusChange = false

            when (player!!.status)
            {
                NOT_READY, MISSING_BEATMAP ->
                {
                    modsButton!!.isVisible = Multiplayer.isRoomHost || room!!.isFreeMods

                    readyButton!!.setText("Ready")
                    readyButton!!.setColor(0.2f, 0.9f, 0.2f)

                    secondaryButton!!.isVisible = true
                    secondaryButton!!.setText("Options")
                    secondaryButton!!.setColor(0.2f, 0.2f, 0.2f)
                }

                READY ->
                {
                    modsButton!!.isVisible = false

                    readyButton!!.setText("Not ready")
                    readyButton!!.setColor(0.9f, 0.2f, 0.2f)

                    secondaryButton!!.isVisible = Multiplayer.isRoomHost

                    if (Multiplayer.isRoomHost)
                    {
                        secondaryButton!!.setText("Start match!")
                        secondaryButton!!.setColor(0.2f, 0.9f, 0.2f)
                    }
                }

                PLAYING -> Unit // Not listening
            }
        }

        // Updating state text
        stateText.text = "${room!!.activePlayers.size} / ${room!!.maxPlayers} players. ${room!!.readyPlayers.size} players ready."

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?)
    {
        // Updating player team
        room!!.getPlayerByUID(uid)!!.team = team

        // Updating player list
        playerList!!.updateItems()
    }

    fun init()
    {
    }
}