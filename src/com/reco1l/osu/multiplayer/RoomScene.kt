package com.reco1l.osu.multiplayer

import android.app.AlertDialog
import com.reco1l.ibancho.IPlayerEventListener
import com.reco1l.ibancho.IRoomEventListener
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.*
import com.reco1l.ibancho.data.PlayerStatus.*
import com.reco1l.ibancho.data.RoomTeam.BLUE
import com.reco1l.ibancho.data.RoomTeam.RED
import com.reco1l.ibancho.data.TeamMode.HEAD_TO_HEAD
import com.reco1l.ibancho.data.TeamMode.TEAM_VS_TEAM
import com.reco1l.ibancho.data.WinCondition.*
import com.reco1l.osu.mainThread
import com.reco1l.osu.multiplayer.Multiplayer.isConnected
import com.reco1l.osu.multiplayer.Multiplayer.isRoomHost
import com.reco1l.osu.multiplayer.Multiplayer.player
import com.reco1l.osu.multiplayer.Multiplayer.room
import com.reco1l.osu.ui.entity.BeatmapButton
import com.reco1l.osu.ui.entity.ComposedText
import com.reco1l.osu.ui.SettingsFragment
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.runSafe
import com.rian.osu.ui.DifficultyAlgorithmSwitcher
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
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.MOD_SCOREV2
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen.LoadingScene
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.SimpleDateFormat
import java.util.*
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.LibraryManager.INSTANCE as library
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.menu.ModMenu.getInstance as getModMenu
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object RoomScene : Scene(), IRoomEventListener, IPlayerEventListener
{

    /**
     * Indicates that the host can change beatmap (it should be false while a change request was done)
     *
     * This is only used if [player] is the room host.
     */
    @JvmField
    var awaitBeatmapChange = false

    /**
     * Indicates that the player can change its status, its purpose is to await server changes.
     */
    @JvmField
    var awaitStatusChange = false

    /**
     * Indicates that the player can change its mods, its purpose is to await server changes.
     */
    @JvmField
    var awaitModsChange = false


    val chat: RoomChat

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

    private var readyButton: TextButton? = null

    private var secondaryButton: TextButton? = null

    private var trackButton: BeatmapButton? = null

    private var modsButton: AnimSprite? = null

    private var difficultySwitcher: DifficultyAlgorithmSwitcher? = null

    private var playerList: RoomPlayerList? = null

    private var settingsFragment: SettingsFragment? = null

    private val onlinePanel = OnlinePanel()

    private val titleText = ChangeableText(20f, 20f, getResources().getFont("bigFont"), "", 100)

    private val stateText = ChangeableText(0f, 0f, getResources().getFont("smallFont"), "", 250)

    private val infoText = ChangeableText(0f, 0f, getResources().getFont("smallFont"), "", 200)


    private val beatmapInfoText = ChangeableText(10f, 10f, getResources().getFont("smallFont"), "", 150)

    private var beatmapInfoRectangle: Rectangle? = null

    init
    {
        RoomAPI.playerEventListener = this
        RoomAPI.roomEventListener = this
        chat = RoomChat()
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

        // Beatmap info
        beatmapInfoRectangle = Rectangle(0f, 0f, trackButton!!.width * 0.75f, 0f).also {
            it.setColor(0f, 0f, 0f, 0.9f)
            it.isVisible = false

            beatmapInfoText.detachSelf()
            it.attachChild(beatmapInfoText)

            attachChild(it)
        }

        OsuSkin.get().getColor("MenuItemDefaultTextColor", BeatmapButton.DEFAULT_TEXT_COLOR).apply(beatmapInfoText)

        // Ready button, this button will switch player status
        readyButton = object : TextButton(getResources().getFont("CaptionFont"), "Ready")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitStatusChange)
                    return false

                getResources().getSound("menuclick")?.play()
                awaitStatusChange = true

                // Switching status
                when (player!!.status)
                {
                    NOT_READY ->
                    {
                        if (room!!.beatmap == null)
                        {
                            ToastLogger.showText("Cannot ready when the host is changing beatmap.", true)
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

                    READY -> invalidateStatus()
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
                    if (!isRoomHost)
                        return false

                    // This can never happen, but we handle it just in case.
                    if (room!!.beatmap == null)
                    {
                        ToastLogger.showText("You must select a beatmap first.", true)
                        return true
                    }

                    // If it's team vs team we check if there's at least one per team
                    if (room!!.teamMode == TEAM_VS_TEAM)
                    {
                        val team = room!!.teamMap

                        if (team[RED].isNullOrEmpty() || team[BLUE].isNullOrEmpty())
                        {
                            ToastLogger.showText("At least 1 player per team is needed to start a match!", true)
                            return true
                        }
                    }

                    // Filtering players that can start the match, we ignore players that doesn't have the beatmap
                    val players = room!!.activePlayers.filter { it.status != MISSING_BEATMAP }

                    // Checking if there's at least 2 players
                    if (players.size <= 1)
                    {
                        ToastLogger.showText("At least 2 players need to have the beatmap!", true)
                        return true
                    }

                    getResources().getSound("menuhit")?.play()
                    RoomAPI.notifyMatchPlay()
                    return true
                }
                else mainThread {
                    settingsFragment = SettingsFragment()
                    settingsFragment!!.show()
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
                        mainThread { leaveDialog.show() }
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
                if (!isRoomHost && !room!!.gameplaySettings.isFreeMod || awaitModsChange || awaitStatusChange || player!!.status == READY)
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
            else it.setPosition(backButton!!.x + backButton!!.width, Config.getRES_HEIGHT() - 90f)
        }

        // Difficulty switcher
        difficultySwitcher = DifficultyAlgorithmSwitcher().also {

            it.setPosition(modsButton!!.x + modsButton!!.widthScaled, Config.getRES_HEIGHT() - it.heightScaled)

            registerTouchArea(it)
            attachChild(it)
        }

        // Online panel
        onlinePanel.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
        attachChild(onlinePanel)

        sortChildren()
    }

    override fun onSceneTouchEvent(event: TouchEvent): Boolean {
        trackButton?.also {
            beatmapInfoRectangle?.isVisible =
                getGlobal().selectedTrack != null &&
                !event.isActionUp &&
                event.x in it.x..it.x + it.width &&
                event.y in it.y..it.y + it.height
        }

        return super.onSceneTouchEvent(event)
    }

    // Update events

    @JvmStatic
    fun updateOnlinePanel() = updateThread {

        onlinePanel.setInfo()
        onlinePanel.setAvatar()
    }

    private fun updateBackground(path: String?)
    {
        val texture = if (path != null && !Config.isSafeBeatmapBg())
            getResources().loadBackground(path) else getResources().getTexture("menu-background")

        val height = texture.height * Config.getRES_WIDTH() / texture.width.toFloat()
        val width = Config.getRES_WIDTH().toFloat()

        background = SpriteBackground(Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, width, height, texture))
    }

    private fun updateInformation()
    {
        updateButtons()

        // Update room name text
        titleText.text = room!!.name

        // Update room state text
        stateText.text = buildString {

            append(room!!.activePlayers.size).append(" / ").append(room!!.maxPlayers).append(" players")
            append(" - ")
            append(room!!.readyPlayers.size).append(" ready")

            // Showing the amount of players per team if it's team vs team mode.
            if (room!!.teamMode == TEAM_VS_TEAM)
            {
                val team = room!!.teamMap

                append(" - ")
                append("Red Team: ").append(team[RED]?.size ?: 0).append(" vs Blue Team: ").append(team[BLUE]?.size ?: 0)
            }
        }

        // Update room info text
        infoText.text = """
            Mods: ${room!!.modsToReadableString()}
            Slider Lock: ${if (room!!.gameplaySettings.isRemoveSliderLock) "Enabled" else "Disabled" }
            Team mode: ${if (room!!.teamMode == HEAD_TO_HEAD) "Head-to-head" else "Team VS"}
            Win condition: ${
                when (room!!.winCondition)
                {
                    SCORE_V1 -> "Score V1"
                    ACCURACY -> "Accuracy"
                    MAX_COMBO -> "Max combo"
                    SCORE_V2 -> "Score V2"
                }
            }
        """.trimIndent()
    }

    private fun updateButtons()
    {
        if (player!!.status == READY)
        {
            readyButton!!.setText("Not ready")
            readyButton!!.setColor(0.9f, 0.2f, 0.2f)

            modsButton!!.isVisible = false
            difficultySwitcher!!.setPosition(modsButton!!.x, difficultySwitcher!!.y)

            secondaryButton!!.isVisible = isRoomHost

            if (isRoomHost)
            {
                room!!.activePlayers.run {
                    val playersReady = filter { it.status == READY }

                    secondaryButton!!.setText(
                        if (playersReady.size == size) "Start Game!"
                        else "Force Start Game! (${playersReady.size}/${size})"
                    )
                }

                secondaryButton!!.setColor(0.2f, 0.9f, 0.2f)
            }
            return
        }

        readyButton!!.setText("Ready")
        readyButton!!.setColor(0.2f, 0.9f, 0.2f)

        secondaryButton!!.isVisible = true
        secondaryButton!!.setText("Options")
        secondaryButton!!.setColor(0.2f, 0.2f, 0.2f)

        modsButton!!.isVisible = isRoomHost || room!!.gameplaySettings.isFreeMod

        if (modsButton!!.isVisible) {
            difficultySwitcher!!.setPosition(modsButton!!.x + modsButton!!.widthScaled, difficultySwitcher!!.y)
        }
    }

    private fun updateBeatmapInfo()
    {
        beatmapInfoRectangle!!.isVisible = getGlobal().selectedTrack?.let { track ->

            beatmapInfoText.text = """
                Length: ${
                    SimpleDateFormat(if (track.musicLength > 3600 * 1000) "HH:mm:ss" else "mm:ss").let {
                        it.timeZone = TimeZone.getTimeZone("GMT+0")
                        it.format(track.musicLength)
                    }
                } BPM: ${
                    if (track.bpmMin == track.bpmMax) 
                        "%.1f".format(track.bpmMin) 
                    else 
                        "%.1f-%.1f".format(track.bpmMin, track.bpmMax)
                } 
                CS: ${track.circleSize} AR: ${track.approachRate} OD: ${track.overallDifficulty} HP: ${track.hpDrain} Star Rating: ${
                    if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) track.standardDifficulty
                    else track.droidDifficulty
                }
            """.trimIndent()

            true
        } ?: false

        beatmapInfoRectangle!!.also { rect ->

            rect.width = beatmapInfoText.width + 20
            rect.height = beatmapInfoText.height + 20

            trackButton!!.let { rect.setPosition(it.x + it.width - rect.width - 20, it.y + it.height) }
        }
    }

    fun switchDifficultyAlgorithm() {
        trackButton!!.updateBeatmap(room!!.beatmap)
        updateBeatmapInfo()
    }

    // Actions

    fun invalidateStatus()
    {
        // Status shouldn't be changed during reconnection because it's done by server, this function can be called by
        // any of the updating functions. Changing status during reconnection can break the reconnection call hierarchy.
        if (Multiplayer.isReconnecting)
            return

        awaitStatusChange = true

        var newStatus = NOT_READY

        if (room!!.beatmap != null && getGlobal().selectedTrack == null)
            newStatus = MISSING_BEATMAP

        if (player!!.status != newStatus)
            RoomAPI.setPlayerStatus(newStatus)
        else
            awaitStatusChange = false
    }

    fun clear()
    {
        room = null
        player = null

        // Clearing chat
        chat.clear()
        chat.dismiss()

        mainThread {
            playerList?.menu?.dismiss()
            settingsFragment?.dismiss()

            updateThread {
                getModMenu().hide()

                playerList?.detachSelf()
                playerList = null
            }
        }
    }


    // Navigation

    override fun back()
    {
        // Stopping the attempt loop if user cancels reconnection.
        Multiplayer.isReconnecting = false

        runSafe { RoomAPI.disconnect() }
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

        if (!isConnected)
        {
            back()
            return
        }

        getGlobal().engine.scene = this

        // Updating beatmap just in case only if there's no await lock.
        if (!awaitBeatmapChange)
            onRoomBeatmapChange(room!!.beatmap)

        // Invalidating status
        invalidateStatus()

        chat.show()
    }


    // Communication

    override fun onServerError(error: String) = ToastLogger.showText(error, true)

    override fun onRoomChatMessage(uid: Long?, message: String)
    {
        // If username is null considering it as system message
        if (uid != null)
        {
            val player = room!!.playersMap[uid] ?: run {

                Multiplayer.log("WARNING: Unable to find user by ID on chat message")
                return
            }

            if (!player.isMuted)
                chat.onRoomChatMessage(player, message)
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
        player = newRoom.playersMap[getOnline().userId]!!

        // Reloading player list
        playerList?.detachSelf()
        playerList = RoomPlayerList(newRoom)
        attachChild(playerList, 1)

        // Reloading mod menu, we set player mods first in case the scene was reloaded (due to skin change).
        clearChildScene()
        getModMenu().setMods(player!!.mods, false, true)
        getModMenu().init()
        getModMenu().setMods(newRoom.mods, newRoom.gameplaySettings.isFreeMod, newRoom.gameplaySettings.allowForceDifficultyStatistics)

        // Updating player mods for other clients
        awaitModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(getModMenu().mod),
            getModMenu().changeSpeed,
            getModMenu().fLfollowDelay,
            getModMenu().customAR,
            getModMenu().customOD,
            getModMenu().customCS,
            getModMenu().customHP
        )

        // Updating UI
        updateInformation()
        playerList!!.invalidate()

        if (Multiplayer.isReconnecting)
        {
            onRoomBeatmapChange(room!!.beatmap)

            Multiplayer.onReconnectAttempt(true)

            // If the status returned by server is PLAYING then it means the match was forced to start while the player
            // was disconnected.
            if (player!!.status == PLAYING)
            {
                // Handling special case when the beatmap could have been changed and match was started while player was
                // disconnected.
                if (getGlobal().selectedTrack != null)
                    onRoomMatchPlay()
                else
                    invalidateStatus()
            }
            return
        }

        show()
    }

    override fun onRoomDisconnect(reason: String?, byUser: Boolean)
    {
        if (!byUser)
        {
            // Setting await locks to avoid player emitting events that will be ignored.
            awaitBeatmapChange = true
            awaitStatusChange = true
            awaitModsChange = true

            chat.onSystemChatMessage("Connection lost, trying to reconnect...", "#FF0000")
            Multiplayer.onReconnect()
            return
        }

        back()
    }

    override fun onRoomConnectFail(error: String?)
    {
        Multiplayer.log("ERROR: Failed to connect -> $error")

        if (Multiplayer.isReconnecting)
        {
            Multiplayer.onReconnectAttempt(false)
            return
        }

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
        // Updating values
        room!!.beatmap = beatmap

        // Searching the beatmap in the library
        getGlobal().selectedTrack = library.findTrackByMD5(beatmap?.md5)

        // Updating track button
        trackButton!!.updateBeatmap(beatmap)

        // Preventing from change song when host is in room while other players are in gameplay
        if (getGlobal().engine.scene != this)
        {
            awaitBeatmapChange = false
            return
        }

        // Notify to the host when other players can't download the beatmap.
        if (isRoomHost && beatmap != null && beatmap.parentSetID == null)
            ToastLogger.showText("This beatmap isn't available on Chimu.", false)

        // Updating player status
        invalidateStatus()

        // Updating background
        updateBackground(getGlobal().selectedTrack?.background)
        updateBeatmapInfo()

        // Releasing await lock
        awaitBeatmapChange = false

        if (getGlobal().selectedTrack == null)
        {
            getGlobal().songService.stop()
            return
        }

        getGlobal().songService.preLoad(getGlobal().selectedTrack.beatmap.music)
        getGlobal().songService.play()
    }

    override fun onRoomHostChange(uid: Long)
    {
        room!!.host = uid

        chat.onSystemChatMessage("Player ${room!!.playersMap[uid]?.name} is now the room host.", "#007BFF")

        // Reloading mod menu
        updateThread {
            getModMenu().hide(false)

            // Reloading buttons sprites
            getModMenu().init()
            getModMenu().update()
        }

        // Updating player list
        playerList!!.invalidate()
    }


    // Mods

    override fun onRoomModsChange(mods: RoomMods)
    {
        if (mods != room!!.mods)
            invalidateStatus()

        room!!.mods = mods

        // If free mods is enabled it'll keep player mods and enforce speed changing mods and ScoreV2.
        // If allow force difficulty statistics is enabled under free mod, the force difficulty statistics settings
        // set by the player will not be overridden.
        getModMenu().setMods(mods, room!!.gameplaySettings.isFreeMod, room!!.gameplaySettings.allowForceDifficultyStatistics)

        // Updating player mods
        awaitModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(getModMenu().mod),
            getModMenu().changeSpeed,
            getModMenu().fLfollowDelay,
            getModMenu().customAR,
            getModMenu().customOD,
            getModMenu().customCS,
            getModMenu().customHP
        )

        // Update room info text
        updateInformation()
    }

    override fun onRoomGameplaySettingsChange(settings: RoomGameplaySettings) {
        room!!.gameplaySettings = settings

        // Update room info text
        updateInformation()

        // Updating player mods
        awaitModsChange = true

        // Hiding mod button in case isn't the host when free mods is disabled
        modsButton!!.isVisible = isRoomHost || settings.isFreeMod

        // Moving difficulty switcher with respect to mods button
        difficultySwitcher!!.setPosition(
            modsButton!!.x + if (modsButton!!.isVisible) modsButton!!.widthScaled else 0f,
            difficultySwitcher!!.y
        )

        // Closing mod menu, to enforce mod menu scene update
        getModMenu().hide(false)

        // Invalidating player status
        invalidateStatus()
    }

    /**This method is used purely to update UI in other clients*/
    override fun onPlayerModsChange(uid: Long, mods: RoomMods)
    {
        // Updating player mods
        room!!.playersMap[uid]!!.mods = mods

        // Updating player list
        playerList!!.invalidate()

        // Removing await lock
        if (uid == player!!.id)
            awaitModsChange = false
    }

    override fun onRoomTeamModeChange(mode: TeamMode)
    {
        room!!.teamMode = mode

        // Update room info text
        updateInformation()

        // Updating player list
        playerList!!.invalidate()

        // Setting player status to NOT_READY
        awaitStatusChange = true

        // Invalidating player status
        invalidateStatus()
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition)
    {
        room!!.winCondition = winCondition

        // Update room info text
        updateInformation()

        if (isRoomHost)
        {
            awaitModsChange = true

            // If win condition is Score V2 we add the mod.
            val roomMods = room!!.mods.set.apply {

                if (winCondition == SCORE_V2)
                    add(MOD_SCOREV2)
                else
                    remove(MOD_SCOREV2)
            }

            // Applying to all room
            RoomAPI.setRoomMods(
                modsToString(roomMods),
                getModMenu().changeSpeed,
                getModMenu().fLfollowDelay,
                getModMenu().customAR,
                getModMenu().customOD,
                getModMenu().customCS,
                getModMenu().customHP
            )
        }

        // Updating player list
        playerList!!.invalidate()

        // Invalidating player status
        invalidateStatus()
    }


    // Match

    override fun onRoomMatchPlay()
    {
        if (player!!.status != MISSING_BEATMAP && getGlobal().engine.scene != getGlobal().gameScene.scene)
        {
            if (getGlobal().selectedTrack == null)
            {
                Multiplayer.log("WARNING: Attempt to start match with null track.")
                return
            }

            getGlobal().songMenu.stopMusic()

            Replay.oldMod = getModMenu().mod
            Replay.oldChangeSpeed = getModMenu().changeSpeed
            Replay.oldFLFollowDelay = getModMenu().fLfollowDelay

            Replay.oldCustomAR = getModMenu().customAR
            Replay.oldCustomOD = getModMenu().customOD
            Replay.oldCustomCS = getModMenu().customCS
            Replay.oldCustomHP = getModMenu().customHP

            getGlobal().gameScene.startGame(getGlobal().selectedTrack, null)

            // Hiding any player menu if its shown
            mainThread { playerList!!.menu.dismiss() }
        }

        // Updating player list
        playerList!!.invalidate()
    }

    override fun onRoomMatchStart()
    {
        if (getGlobal().engine.scene is LoadingScene)
            getGlobal().gameScene.start()

        // Updating player list
        playerList!!.invalidate()
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
        playerList!!.invalidate()
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
        playerList!!.invalidate()
    }

    override fun onPlayerKick(uid: Long)
    {
        if (uid == player!!.id)
        {
            Multiplayer.log("Kicked from room.")

            if (getGlobal().engine.scene == getGlobal().gameScene.scene) {
                ToastLogger.showText("You were kicked by the room host, but you can continue playing.", true)
                return
            }

            back()
            mainThread {
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
        playerList!!.invalidate()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus)
    {
        // Updating player status
        room!!.playersMap[uid]!!.status = status

        if (uid == player!!.id)
            awaitStatusChange = false

        // Updating state text
        updateInformation()

        // Updating player list
        playerList!!.invalidate()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?)
    {
        // Updating player team
        room!!.playersMap[uid]!!.team = team

        // Updating player list
        playerList!!.invalidate()

        // Update information text
        updateInformation()
    }

    fun init() = Unit
}