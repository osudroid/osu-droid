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
import com.reco1l.framework.extensions.orCatch
import com.reco1l.framework.lang.glThread
import com.reco1l.framework.lang.uiThread
import com.reco1l.legacy.data.modsToReadable
import com.reco1l.legacy.data.modsToString
import com.reco1l.legacy.data.stringToMods
import com.reco1l.legacy.ui.entity.BeatmapButton
import com.reco1l.legacy.ui.entity.ComposedText
import org.anddev.andengine.engine.camera.SmoothCamera
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


    init
    {
        RoomAPI.playerEventListener = this
        RoomAPI.roomEventListener = this
    }


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
                            ToastLogger.showText("Cannot ready the when host is changing beatmap.", true)
                            awaitStatusChange = false
                            return true
                        }

                        if (room!!.teamMode == TEAM_VS_TEAM && player!!.team == null)
                        {
                            ToastLogger.showText("You must select a team first!", true)
                            awaitStatusChange = false
                            return true
                        }
                        RoomAPI.setPlayerStatus(READY)
                    }

                    READY -> RoomAPI.setPlayerStatus(NOT_READY)
                    MISSING_BEATMAP ->
                    {
                        ToastLogger.showText("Beatmap is missing, cannot ready.", true)
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

                    // This can never happen, but we handle it just in case.
                    if (room!!.beatmap == null)
                    {
                        ToastLogger.showText("You must select a beatmap first.", true)
                        return true
                    }

                    // Filtering host from the array
                    val playersWithBeatmap = room!!.playersWithBeatmap.filterNot { it == player }

                    if (room!!.teamMode == TEAM_VS_TEAM)
                    {
                        if (room!!.redTeamPlayers.isEmpty() || room!!.blueTeamPlayers.isEmpty())
                        {
                            ToastLogger.showText("At least 1 player per team is needed to start a match!", true)
                            return true
                        }
                    }

                    // Checking if all players that have the beatmap are READY.
                    if (playersWithBeatmap.all { it.status == READY })
                    {
                        RoomAPI.notifyMatchPlay()
                        return true
                    }
                    ToastLogger.showText("All players with the beatmap need to be ready!", true)
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

            glThread {
                it.setInfo()
                it.setAvatar()
            }
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
        // Update room name text
        titleText.text = room!!.name

        // Update room state text
        stateText.text = "${room!!.activePlayers.size} / ${room!!.maxPlayers} players. ${room!!.readyPlayers.size} ${if (room!!.readyPlayers.size == 1) "player" else "players"} ready."

        // Update room info text
        infoText.text = """
            Host: ${room!!.hostPlayer?.name}
            Team mode: ${
            when (room!!.teamMode)
            {
                HEAD_TO_HEAD -> "Head-to-head"
                TEAM_VS_TEAM -> "Team VS"
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

    private fun updateButtons()
    {
        if (player!!.status == READY)
        {
            readyButton!!.setText("Not ready")
            readyButton!!.setColor(0.9f, 0.2f, 0.2f)

            modsButton!!.isVisible = false
            secondaryButton!!.isVisible = Multiplayer.isRoomHost

            if (Multiplayer.isRoomHost)
            {
                secondaryButton!!.setText("Start match!")
                secondaryButton!!.setColor(0.2f, 0.9f, 0.2f)
            }
            return
        }

        readyButton!!.setText("Ready")
        readyButton!!.setColor(0.2f, 0.9f, 0.2f)

        secondaryButton!!.isVisible = true
        secondaryButton!!.setText("Options")
        secondaryButton!!.setColor(0.2f, 0.2f, 0.2f)

        modsButton!!.isVisible = Multiplayer.isRoomHost || room!!.isFreeMods
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
        (getGlobal().camera as SmoothCamera).apply {

            setZoomFactorDirect(1f)

            if (Config.isShrinkPlayfieldDownwards())
                setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        }

        if (!Multiplayer.isConnected)
        {
            back()
            return
        }

        getGlobal().engine.scene = this

        // Updating beatmap just in case
        onRoomBeatmapChange(room!!.beatmap)

        // Setting player status to NOT_READY
        awaitStatusChange = true

        if (hasLocalTrack || room!!.beatmap == null)
            RoomAPI.setPlayerStatus(NOT_READY)
        else
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)

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

        // Finding our player object
        player = newRoom.getPlayerByUID(getOnline().userId) ?: run {

            ToastLogger.showText("Unable to find player", false)
            back()
            return
        }

        // Determine if it's the host
        Multiplayer.isRoomHost = player!!.id == newRoom.host

        // Reloading player list
        playerList?.detachSelf()
        playerList = RoomPlayerList(newRoom)
        attachChild(playerList, 1)

        // Reloading mod menu
        getModMenu().reload()
        getModMenu().setMods(stringToMods(newRoom.mods), newRoom.isFreeMods)

        // Updating player mods for other clients
        awaitModsChange = true
        RoomAPI.setPlayerMods(modsToString(getModMenu().mod))

        // Updating UI
        updateButtons()
        updateInformation()
        playerList!!.updateItems()

        show()
    }

    override fun onRoomDisconnect()
    {
        room = null
        ToastLogger.showText("Disconnected from the room", true)

        // If player is in one of these scenes we go back.
        if (getGlobal().engine.scene != getGlobal().gameScene.scene)
            back()
        else
            multiLog("Disconnected from socket while playing.")
    }

    override fun onRoomConnectFail(error: String?)
    {
        room = null
        ToastLogger.showText("Failed to connect to the room: $error", true)
        back()
    }


    // Room changes

    override fun onRoomNameChange(name: String)
    {
        room!!.name = name
        updateInformation()
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?)
    {
        // Setting await lock
        awaitStatusChange = true

        // Updating values
        room!!.beatmap = beatmap
        trackButton!!.beatmap = beatmap

        // Searching if the track is in our library
        val localTrack = beatmap?.let { library.findTrackByMD5(it.md5) }

        if (localTrack == null)
            multiLog("The beatmap was not found in local library.")

        // Updating player status.
        if (localTrack != null || beatmap == null)
            RoomAPI.setPlayerStatus(NOT_READY)
        else
            RoomAPI.setPlayerStatus(MISSING_BEATMAP)

        // Updating track button.
        if (beatmap != null)
            trackButton!!.loadTrack(localTrack)

        // Preventing from change song when host is in room while other players are in gameplay
        if (getGlobal().engine.scene != this)
        {
            awaitBeatmapChange = false
            return
        }

        // Updating background
        updateBackground(localTrack?.background)

        // Releasing await lock
        awaitBeatmapChange = false

        // Playing selected track
        getGlobal().selectedTrack = localTrack

        if (localTrack == null)
        {
            getGlobal().songService.stop()
            return
        }

        getGlobal().songService.preLoad(localTrack.beatmap.music)
        getGlobal().songService.play()
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
            getModMenu().update()
        }

        // Updating buttons visibility
        updateButtons()

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
        if (player!!.status == READY)
        {
            getGlobal().songMenu.stopMusic()

            Replay.oldMod = getModMenu().mod
            Replay.oldChangeSpeed = getModMenu().changeSpeed
            Replay.oldForceAR = getModMenu().forceAR
            Replay.oldEnableForceAR = getModMenu().isEnableForceAR
            Replay.oldFLFollowDelay = getModMenu().fLfollowDelay

            getGlobal().songMenu.game.startGame(getGlobal().selectedTrack, null)

            // Hiding any player menu if its shown
            uiThread { playerList!!.menu.dismiss() }
        }

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onRoomMatchStart()
    {
        if (getGlobal().engine.scene is LoadingScene)
            getGlobal().gameScene.start()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onRoomMatchSkip()
    {
        if (getGlobal().engine.scene != getGlobal().gameScene.scene)
            return

        getGlobal().gameScene.skip()
    }


    // Leaderboard

    override fun onRoomLiveLeaderboard(leaderboard: JSONArray) = Multiplayer.onLiveLeaderboard(leaderboard)

    override fun onRoomFinalLeaderboard(leaderboard: JSONArray) = Multiplayer.onFinalLeaderboard(leaderboard)


    // Player related events

    override fun onPlayerJoin(player: RoomPlayer)
    {
        // We send the message if the player wasn't in the room, sometimes this event can be called by a reconnection.
        if (room!!.addPlayer(player))
            chat.onSystemChatMessage("Player ${player.name} (ID: ${player.id}) joined.", "#007BFF")

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerLeft(uid: Long)
    {
        val player = room!!.removePlayer(uid)

        // Notifying in chat
        if (player != null)
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) left.", "#007BFF")

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.updateItems()
    }

    override fun onPlayerKick(uid: Long)
    {
        if (uid == player!!.id)
        {
            multiLog("Kicked from room.")
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

        val player = room!!.removePlayer(uid)

        // Notifying in chat
        if (player != null)
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) was kicked.", "#FF0000")

        // Updating state text
        updateInformation()

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
            updateButtons()
        }

        // Updating state text
        updateInformation()

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

    fun init() = Unit
}