package com.reco1l.osu.multiplayer

import com.reco1l.andengine.*
import com.reco1l.andengine.sprite.*
import com.reco1l.ibancho.IPlayerEventListener
import com.reco1l.ibancho.IRoomEventListener
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.*
import com.reco1l.ibancho.data.PlayerStatus.*
import com.reco1l.ibancho.data.RoomTeam.Blue
import com.reco1l.ibancho.data.RoomTeam.Red
import com.reco1l.ibancho.data.TeamMode.HeadToHead
import com.reco1l.ibancho.data.TeamMode.TeamVersus
import com.reco1l.ibancho.data.WinCondition.*
import com.reco1l.osu.mainThread
import com.reco1l.osu.multiplayer.Multiplayer.isConnected
import com.reco1l.osu.multiplayer.Multiplayer.isRoomHost
import com.reco1l.osu.multiplayer.Multiplayer.player
import com.reco1l.osu.multiplayer.Multiplayer.room
import com.reco1l.osu.ui.MessageDialog
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
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.MOD_SCOREV2
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen.LoadingScene
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.SimpleDateFormat
import java.util.*
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object RoomScene : Scene(), IRoomEventListener, IPlayerEventListener {

    /**
     * Indicates that the host can change beatmap (it should be false while a change request was done)
     *
     * This is only used if [player] is the room host.
     */
    @JvmField
    var isWaitingForBeatmapChange = false

    /**
     * Indicates that the player can change its status, its purpose is to await server changes.
     */
    @JvmField
    var isWaitingForStatusChange = false

    /**
     * Indicates that the player can change its mods, its purpose is to await server changes.
     */
    @JvmField
    var isWaitingForModsChange = false


    /**
     * The room chat.
     */
    val chat: RoomChat

    /**
     * The chat preview text that shows the last message sent in the chat.
     */
    val chatPreviewText = ComposedText(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), 100)

    /**
     * The leave dialog that will be shown when the player tries to leave the room.
     */
    val leaveDialog = MessageDialog().apply {

        setTitle("Leave room")
        setMessage("Are you sure?")
        addButton("Yes") {

            it.dismiss()
            back()
        }

        addButton("No") {
            it.dismiss()
        }

    }


    private var backButton: AnimatedSprite? = null

    private var readyButton: TextButton? = null

    private var secondaryButton: TextButton? = null

    private var beatmapInfoButton: BeatmapButton? = null

    private var modsButton: ExtendedSprite? = null

    private var difficultySwitcher: DifficultyAlgorithmSwitcher? = null

    private var playerList: RoomPlayerList? = null

    private var settingsFragment: SettingsFragment? = null

    private val userCard = OnlinePanel()

    private val roomTitleText = ChangeableText(20f, 20f, ResourceManager.getInstance().getFont("bigFont"), "", 100)

    private val roomStatusText = ChangeableText(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), "", 250)

    private val roomInformationText = ChangeableText(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), "", 200)


    private val beatmapInfoText = ChangeableText(10f, 10f, ResourceManager.getInstance().getFont("smallFont"), "", 150)

    private var beatmapInfoRectangle: Rectangle? = null


    init {
        RoomAPI.playerEventListener = this
        RoomAPI.roomEventListener = this
        chat = RoomChat()
    }


    fun load() {
        detachChildren()
        clearTouchAreas()

        isBackgroundEnabled = true

        val dimRectangle = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        dimRectangle.setColor(0f, 0f, 0f, 0.5f)
        attachChild(dimRectangle, 0)

        val topBarRectangle = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 120f)
        topBarRectangle.setColor(0f, 0f, 0f, 0.3f)
        attachChild(topBarRectangle)

        attachChild(roomTitleText)

        roomStatusText.setPosition(20f, roomTitleText.y + roomTitleText.height)
        roomStatusText.setColor(0.8f, 0.8f, 0.8f)
        attachChild(roomStatusText)

        beatmapInfoButton = BeatmapButton().also {

            it.setPosition(Config.getRES_WIDTH() - it.width + 20f, 130f + 40f)

            registerTouchArea(it)
            attachChild(it)
        }

        chatPreviewText.setPosition(beatmapInfoButton!!.x + 20f, 130f + 10f)
        attachChild(chatPreviewText)

        roomInformationText.setPosition(beatmapInfoButton!!.x + 20f, beatmapInfoButton!!.y + beatmapInfoButton!!.height + 10f)
        attachChild(roomInformationText)

        beatmapInfoRectangle = Rectangle(0f, 0f, beatmapInfoButton!!.width * 0.75f, 0f).also { beatmapInfoRectangle ->
            beatmapInfoRectangle.setColor(0f, 0f, 0f, 0.9f)
            beatmapInfoRectangle.isVisible = false

            beatmapInfoText.detachSelf()
            beatmapInfoRectangle.attachChild(beatmapInfoText)

            attachChild(beatmapInfoRectangle)
        }

        OsuSkin.get().getColor("MenuItemDefaultTextColor", RGBColor(1f, 1f, 1f)).apply(beatmapInfoText)


        readyButton = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"), "Ready") {

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (!event.isActionUp || isWaitingForStatusChange) {
                    return false
                }

                ResourceManager.getInstance().getSound("menuclick")?.play()
                isWaitingForStatusChange = true

                when (player!!.status) {

                    NotReady -> {
                        if (room!!.beatmap == null) {
                            ToastLogger.showText("Cannot ready when the host is changing beatmap.", true)
                            isWaitingForStatusChange = false
                            return true
                        }

                        if (room!!.teamMode == TeamVersus && player!!.team == null) {
                            ToastLogger.showText("You must select a team first!", true)
                            isWaitingForStatusChange = false
                            return true
                        }

                        RoomAPI.setPlayerStatus(Ready)
                    }

                    Ready -> invalidateStatus()

                    MissingBeatmap -> {
                        ToastLogger.showText("Beatmap is missing, cannot ready.", true)
                        isWaitingForStatusChange = false
                    }

                    else -> isWaitingForStatusChange = false /*This case can never happen, the PLAYING status is set when a game starts*/
                }
                return true
            }
        }.also { readyButton ->

            readyButton.width = 400f
            readyButton.setColor(0.2f, 0.9f, 0.2f)
            readyButton.setPosition(Config.getRES_WIDTH() - readyButton.width - 20f, Config.getRES_HEIGHT() - readyButton.height - 20f)

            registerTouchArea(readyButton)
            attachChild(readyButton)
        }

        // It'll only be shown if the player is the room host, if host status is set to READY, this button will start
        // the game otherwise it'll the options button.
        secondaryButton = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"), "Options") {

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (!event.isActionUp || isWaitingForStatusChange) {
                    return false
                }

                if (player!!.status == Ready) {

                    if (!isRoomHost) {
                        return false
                    }

                    if (room!!.beatmap == null) {
                        ToastLogger.showText("You must select a beatmap first.", true)
                        return true
                    }

                    if (room!!.teamMode == TeamVersus) {
                        val team = room!!.teamMap

                        if (team[Red].isNullOrEmpty() || team[Blue].isNullOrEmpty()) {
                            ToastLogger.showText("At least 1 player per team is needed to start a match!", true)
                            return true
                        }
                    }

                    val players = room!!.activePlayers.filter { it.status != MissingBeatmap }

                    if (players.size <= 1) {
                        ToastLogger.showText("At least 2 players need to have the beatmap!", true)
                        return true
                    }

                    ResourceManager.getInstance().getSound("menuhit")?.play()
                    RoomAPI.notifyMatchPlay()
                    return true

                } else {
                    mainThread {
                        settingsFragment = SettingsFragment()
                        settingsFragment!!.show()
                    }
                }
                return false
            }
        }.also { secondaryButton ->

            secondaryButton.width = 400f
            secondaryButton.setColor(0.2f, 0.2f, 0.2f)
            secondaryButton.setPosition(Config.getRES_WIDTH() - secondaryButton.width - 20f, readyButton!!.y - secondaryButton.height - 20f)

            registerTouchArea(secondaryButton)
            attachChild(secondaryButton)
        }


        val isNewLayout = OsuSkin.get().isUseNewLayout
        val layoutBackButton = OsuSkin.get().getLayout("BackButton")
        val layoutMods = OsuSkin.get().getLayout("ModsButton")
        val layoutDifficultySwitcher = OsuSkin.get().getLayout("DifficultySwitcher")

        backButton = object : AnimatedSprite("menu-back", true, OsuSkin.get().animationFramerate) {

            var scaleWhenHold = layoutBackButton?.property?.optBoolean("scaleWhenHold", true) ?: false
            var moved = false
            var dx = 0f
            var dy = 0f


            init {
                autoSizeAxes = Axes.None
            }


            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (event.isActionDown) {

                    if (scaleWhenHold) {
                        setScale(1.25f)
                    }

                    moved = false
                    dx = localX
                    dy = localY

                    ResourceManager.getInstance().getSound("menuback")?.play()
                    return true
                }

                if (event.isActionUp) {
                    setScale(1f)

                    if (!moved) {
                        mainThread {
                            leaveDialog.show()
                        }
                    }
                    return true
                }

                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50) {
                    setScale(1f)
                    moved = true
                }
                return false
            }
        }.also {
            if (isNewLayout && layoutBackButton != null) {
                layoutBackButton.apply(it)
            } else {
                it.setPosition(0f, Config.getRES_HEIGHT() - it.heightScaled)
            }

            registerTouchArea(it)
            attachChild(it)
        }

        modsButton = object : ExtendedSprite() {

            var moved = false
            var dx = 0f
            var dy = 0f


            init {
                textureRegion = ResourceManager.getInstance().getTextureIfLoaded("selection-mods")
                autoSizeAxes = Axes.None
            }


            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (!isRoomHost && !room!!.gameplaySettings.isFreeMod || isWaitingForModsChange || isWaitingForStatusChange || player!!.status == Ready) {
                    return true
                }

                if (event.isActionDown) {
                    moved = false
                    dx = localX
                    dy = localY

                    textureRegion = ResourceManager.getInstance().getTextureIfLoaded("selection-mods-over")
                    return true
                }

                if (event.isActionUp) {
                    textureRegion = ResourceManager.getInstance().getTextureIfLoaded("selection-mods")

                    if (!moved) {
                        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                        ModMenu.getInstance().show(this@RoomScene, GlobalManager.getInstance().selectedBeatmap)
                    }
                    return true
                }

                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50) {

                    if (!moved) {
                        ResourceManager.getInstance().getSound("click-short")?.play()
                    }

                    textureRegion = ResourceManager.getInstance().getTextureIfLoaded("selection-mods")
                    moved = true
                }
                return false
            }
        }.also { modsButton ->

            modsButton.isVisible = false
            modsButton.setScale(1.5f)

            if (isNewLayout && layoutMods != null) {
                layoutMods.apply(modsButton, backButton)
            } else {
                modsButton.setPosition(backButton!!.x + backButton!!.widthScaled, Config.getRES_HEIGHT() - modsButton.heightScaled)
            }

            registerTouchArea(modsButton)
            attachChild(modsButton)
        }

        difficultySwitcher = DifficultyAlgorithmSwitcher().also { difficultySwitcher ->
            difficultySwitcher.setScale(1.5f)

            if (isNewLayout && layoutDifficultySwitcher != null) {
                layoutDifficultySwitcher.apply(difficultySwitcher, modsButton)
            } else {
                difficultySwitcher.setPosition(modsButton!!.x + modsButton!!.widthScaled, Config.getRES_HEIGHT() - difficultySwitcher.heightScaled)
            }

            registerTouchArea(difficultySwitcher)
            attachChild(difficultySwitcher)
        }

        userCard.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
        attachChild(userCard)

        sortChildren()
    }

    override fun onSceneTouchEvent(event: TouchEvent): Boolean {

        beatmapInfoButton?.also {
            beatmapInfoRectangle?.isVisible = GlobalManager.getInstance().selectedBeatmap != null
                && !event.isActionUp
                && event.x in it.x..it.x + it.width
                && event.y in it.y..it.y + it.height
        }

        return super.onSceneTouchEvent(event)
    }


    // Update events

    @JvmStatic
    fun updateOnlinePanel() = updateThread {
        userCard.setInfo()
        userCard.setAvatar()
    }

    private fun updateBackground(path: String?) {

        val texture = if (path != null && !Config.isSafeBeatmapBg()) {
            ResourceManager.getInstance().loadBackground(path)
        } else {
            ResourceManager.getInstance().getTexture("menu-background")
        }

        val height = texture.height * Config.getRES_WIDTH() / texture.width.toFloat()
        val width = Config.getRES_WIDTH().toFloat()

        background = SpriteBackground(Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, width, height, texture))
    }

    private fun updateInformation() {

        updateButtons()

        roomTitleText.text = room!!.name
        roomStatusText.text = buildString {

            append(room!!.activePlayers.size).append(" / ").append(room!!.maxPlayers).append(" players")
            append(" - ")
            append(room!!.readyPlayers.size).append(" ready")

            if (room!!.teamMode == TeamVersus) {
                val team = room!!.teamMap

                append(" - ")
                append("Red Team: ").append(team[Red]?.size ?: 0).append(" vs Blue Team: ").append(team[Blue]?.size ?: 0)
            }
        }

        roomInformationText.text = """
            Mods: ${room!!.modsToReadableString()}
            Slider Lock: ${if (room!!.gameplaySettings.isRemoveSliderLock) "Enabled" else "Disabled"}
            Team mode: ${if (room!!.teamMode == HeadToHead) "Head-to-head" else "Team VS"}
            Win condition: ${when (room!!.winCondition) {
                ScoreV1 -> "Score V1"
                HighestAccuracy -> "Accuracy"
                MaximumCombo -> "Max combo"
                ScoreV2 -> "Score V2"
            }}
        """.trimIndent()
    }

    private fun updateButtons() {

        if (player!!.status == Ready) {

            readyButton!!.setText("Not ready")
            readyButton!!.setColor(0.9f, 0.2f, 0.2f)

            modsButton!!.isVisible = false
            difficultySwitcher!!.setPosition(modsButton!!.x, difficultySwitcher!!.y)

            secondaryButton!!.isVisible = isRoomHost

            if (isRoomHost) {

                room!!.activePlayers.run {
                    val playersReady = filter { it.status == Ready }

                    secondaryButton!!.setText(when (playersReady.size) {
                        size -> "Start Game!"
                        else -> "Force Start Game! (${playersReady.size}/${size})"
                    })
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

    private fun updateBeatmapInfo() {

        beatmapInfoRectangle!!.isVisible = GlobalManager.getInstance().selectedBeatmap?.let { beatmapInfo ->

            beatmapInfoText.text = """
                Length: ${
                    SimpleDateFormat(if (beatmapInfo.length > 3600 * 1000) "HH:mm:ss" else "mm:ss").let {
                        it.timeZone = TimeZone.getTimeZone("GMT+0")
                        it.format(beatmapInfo.length)
                    }
                } 
                BPM: ${
                    if (beatmapInfo.bpmMin == beatmapInfo.bpmMax) {
                        "%.1f".format(beatmapInfo.bpmMin)
                    } else {
                        "%.1f-%.1f".format(beatmapInfo.bpmMin, beatmapInfo.bpmMax)
                    }
                } 
                CS: ${beatmapInfo.circleSize} AR: ${beatmapInfo.approachRate} OD: ${beatmapInfo.overallDifficulty} HP: ${beatmapInfo.hpDrainRate} Star Rating: ${beatmapInfo.getStarRating()}
            """.trimIndent()

            true
        } ?: false

        beatmapInfoRectangle!!.also { rect ->

            rect.width = beatmapInfoText.width + 20
            rect.height = beatmapInfoText.height + 20

            beatmapInfoButton!!.let { rect.setPosition(it.x + it.width - rect.width - 20, it.y + it.height) }
        }
    }

    fun switchDifficultyAlgorithm() {
        beatmapInfoButton!!.updateBeatmap(room!!.beatmap)
        updateBeatmapInfo()
    }


    // Actions

    fun invalidateStatus() {

        // Status shouldn't be changed during reconnection because it's done by server, this function can be called by
        // any of the updating functions. Changing status during reconnection can break the reconnection call hierarchy.
        if (Multiplayer.isReconnecting) {
            return
        }

        isWaitingForStatusChange = true

        var newStatus = NotReady

        if (room!!.beatmap != null && GlobalManager.getInstance().selectedBeatmap == null) {
            newStatus = MissingBeatmap
        }

        if (player!!.status != newStatus) {
            RoomAPI.setPlayerStatus(newStatus)
        } else {
            isWaitingForStatusChange = false
        }
    }

    fun clear() {
        room = null
        player = null

        chat.clear()
        chat.dismiss()

        mainThread {
            playerList?.menu?.dismiss()

            updateThread {
                ModMenu.getInstance().hide()

                playerList?.detachSelf()
                playerList = null
            }
        }
    }


    // Navigation

    override fun back() {
        Multiplayer.isReconnecting = false

        runSafe { RoomAPI.disconnect() }
        clear()
        LobbyScene.show()
    }

    fun show() {

        (GlobalManager.getInstance().camera as SmoothCamera).apply {
            setZoomFactorDirect(1f)
            if (Config.isShrinkPlayfieldDownwards()) {
                setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
            }
        }

        if (!isConnected) {
            back()
            return
        }

        GlobalManager.getInstance().engine.scene = this

        if (!isWaitingForBeatmapChange) {
            onRoomBeatmapChange(room!!.beatmap)
        }

        invalidateStatus()
        chat.show()
    }


    // Communication

    override fun onServerError(error: String) {
        ToastLogger.showText(error, true)
    }

    override fun onRoomChatMessage(uid: Long?, message: String) {

        if (uid != null) {

            val player = room!!.playersMap[uid] ?: run {

                Multiplayer.log("WARNING: Unable to find user by ID on chat message")
                return
            }

            if (!player.isMuted) {
                chat.onRoomChatMessage(player, message)
            }

        } else {
            chat.onSystemChatMessage(message, "#459FFF")
        }
    }


    // Connection

    override fun onRoomConnect(newRoom: Room) {

        if (room != newRoom) {
            chat.onSystemChatMessage("Welcome to osu!droid multiplayer", "#459FFF")
        }

        room = newRoom

        isWaitingForModsChange = false
        isWaitingForBeatmapChange = false
        isWaitingForStatusChange = false

        player = newRoom.playersMap[OnlineManager.getInstance().userId]!!

        playerList?.detachSelf()
        playerList = RoomPlayerList(newRoom)
        attachChild(playerList, 1)

        clearChildScene()
        ModMenu.getInstance().init()
        ModMenu.getInstance().setMods(player!!.mods, false, true)
        ModMenu.getInstance().setMods(newRoom.mods, newRoom.gameplaySettings.isFreeMod, newRoom.gameplaySettings.allowForceDifficultyStatistics)

        isWaitingForModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(ModMenu.getInstance().mod),
            ModMenu.getInstance().changeSpeed,
            ModMenu.getInstance().fLfollowDelay,
            ModMenu.getInstance().customAR,
            ModMenu.getInstance().customOD,
            ModMenu.getInstance().customCS,
            ModMenu.getInstance().customHP
        )

        updateInformation()
        playerList!!.invalidate()

        if (Multiplayer.isReconnecting) {
            onRoomBeatmapChange(room!!.beatmap)

            Multiplayer.onReconnectAttempt(true)

            // If the status returned by server is PLAYING then it means the match was forced to start while the player
            // was disconnected.
            if (player!!.status == Playing) {
                // Handling special case when the beatmap could have been changed and match was started while player was
                // disconnected.
                if (GlobalManager.getInstance().selectedBeatmap != null) {
                    onRoomMatchPlay()
                } else {
                    invalidateStatus()
                }
            }
            return
        }

        show()
    }

    override fun onRoomDisconnect(reason: String?, byUser: Boolean) {

        if (!byUser) {
            // Setting await locks to avoid player emitting events that will be ignored.
            isWaitingForBeatmapChange = true
            isWaitingForStatusChange = true
            isWaitingForModsChange = true

            chat.onSystemChatMessage("Connection lost, trying to reconnect...", "#FFBFBF")
            Multiplayer.onReconnect()
            return
        }

        back()
    }

    override fun onRoomConnectFail(error: String?) {
        Multiplayer.log("ERROR: Failed to connect -> $error")

        if (Multiplayer.isReconnecting) {
            Multiplayer.onReconnectAttempt(false)
            return
        }

        back()
    }


    // Room changes

    override fun onRoomNameChange(name: String) {
        room!!.name = name
        updateInformation()
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?) {

        room!!.beatmap = beatmap

        GlobalManager.getInstance().selectedBeatmap = LibraryManager.findBeatmapByMD5(beatmap?.md5)

        beatmapInfoButton!!.updateBeatmap(beatmap)

        if (GlobalManager.getInstance().engine.scene != this) {
            isWaitingForBeatmapChange = false
            return
        }

        // Notify to the host when other players can't download the beatmap.
        if (isRoomHost && beatmap != null && beatmap.parentSetID == null) {
            ToastLogger.showText("This beatmap isn't available in the download mirror servers.", false)
        }

        invalidateStatus()

        updateBackground(GlobalManager.getInstance().selectedBeatmap?.backgroundPath)
        updateBeatmapInfo()

        isWaitingForBeatmapChange = false

        if (GlobalManager.getInstance().selectedBeatmap == null) {
            GlobalManager.getInstance().songService.stop()
            return
        }

        GlobalManager.getInstance().songService.preLoad(GlobalManager.getInstance().selectedBeatmap!!.audioPath)
        GlobalManager.getInstance().songService.play()
    }

    override fun onRoomHostChange(uid: Long) {

        room!!.host = uid

        chat.onSystemChatMessage("Player ${room!!.playersMap[uid]?.name} is now the room host.", "#459FFF")

        updateThread {
            ModMenu.getInstance().hide(false)
            ModMenu.getInstance().init()
            ModMenu.getInstance().update()
        }

        playerList!!.invalidate()
    }


    // Mods

    override fun onRoomModsChange(mods: RoomMods) {

        if (mods != room!!.mods) {
            invalidateStatus()
        }

        room!!.mods = mods

        // If free mods is enabled it'll keep player mods and enforce speed changing mods and ScoreV2.
        // If allow force difficulty statistics is enabled under free mod, the force difficulty statistics settings
        // set by the player will not be overridden.
        ModMenu.getInstance().setMods(mods, room!!.gameplaySettings.isFreeMod, room!!.gameplaySettings.allowForceDifficultyStatistics)

        isWaitingForModsChange = true

        RoomAPI.setPlayerMods(
            modsToString(ModMenu.getInstance().mod),
            ModMenu.getInstance().changeSpeed,
            ModMenu.getInstance().fLfollowDelay,
            ModMenu.getInstance().customAR,
            ModMenu.getInstance().customOD,
            ModMenu.getInstance().customCS,
            ModMenu.getInstance().customHP
        )

        updateInformation()
    }

    override fun onRoomGameplaySettingsChange(settings: RoomGameplaySettings) {

        room!!.gameplaySettings = settings

        updateInformation()

        isWaitingForModsChange = true

        modsButton!!.isVisible = isRoomHost || settings.isFreeMod
        difficultySwitcher!!.setPosition(modsButton!!.x + if (modsButton!!.isVisible) modsButton!!.widthScaled else 0f, difficultySwitcher!!.y)

        ModMenu.getInstance().hide(false)
        invalidateStatus()
    }

    /**This method is used purely to update UI in other clients*/
    override fun onPlayerModsChange(uid: Long, mods: RoomMods) {

        room!!.playersMap[uid]!!.mods = mods

        playerList!!.invalidate()

        if (uid == player!!.id) {
            isWaitingForModsChange = false
        }
    }

    override fun onRoomTeamModeChange(mode: TeamMode) {

        room!!.teamMode = mode

        updateInformation()
        playerList!!.invalidate()

        isWaitingForStatusChange = true
        invalidateStatus()
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition) {

        room!!.winCondition = winCondition

        updateInformation()

        if (isRoomHost) {
            isWaitingForModsChange = true

            val roomMods = room!!.mods.set.apply {

                if (winCondition == ScoreV2) {
                    add(MOD_SCOREV2)
                } else {
                    remove(MOD_SCOREV2)
                }
            }

            RoomAPI.setRoomMods(
                modsToString(roomMods),
                ModMenu.getInstance().changeSpeed,
                ModMenu.getInstance().fLfollowDelay,
                ModMenu.getInstance().customAR,
                ModMenu.getInstance().customOD,
                ModMenu.getInstance().customCS,
                ModMenu.getInstance().customHP
            )
        }

        playerList!!.invalidate()
        invalidateStatus()
    }


    // Match

    override fun onRoomMatchPlay() {

        if (player!!.status != MissingBeatmap && GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {

            if (GlobalManager.getInstance().selectedBeatmap == null) {
                Multiplayer.log("WARNING: Attempt to start match with null track.")
                return
            }

            GlobalManager.getInstance().songMenu.stopMusic()

            Replay.oldMod = ModMenu.getInstance().mod
            Replay.oldChangeSpeed = ModMenu.getInstance().changeSpeed
            Replay.oldFLFollowDelay = ModMenu.getInstance().fLfollowDelay

            Replay.oldCustomAR = ModMenu.getInstance().customAR
            Replay.oldCustomOD = ModMenu.getInstance().customOD
            Replay.oldCustomCS = ModMenu.getInstance().customCS
            Replay.oldCustomHP = ModMenu.getInstance().customHP

            GlobalManager.getInstance().gameScene.startGame(GlobalManager.getInstance().selectedBeatmap, null)

            mainThread {
                playerList!!.menu.dismiss()
            }
        }

        playerList!!.invalidate()
    }

    override fun onRoomMatchStart() {

        if (GlobalManager.getInstance().engine.scene is LoadingScene) {
            GlobalManager.getInstance().gameScene.start()
        }

        playerList!!.invalidate()
    }

    override fun onRoomMatchSkip() {

        if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
            return
        }

        GlobalManager.getInstance().gameScene.skip()
    }


    // Leaderboard

    override fun onRoomLiveLeaderboard(leaderboard: JSONArray) {
        Multiplayer.onLiveLeaderboard(leaderboard)
    }

    override fun onRoomFinalLeaderboard(leaderboard: JSONArray) {
        Multiplayer.onFinalLeaderboard(leaderboard)
    }


    // Player related events

    override fun onPlayerJoin(player: RoomPlayer) {

        // We send the message if the player wasn't in the room, sometimes this event can be called by a reconnection.
        if (room!!.addPlayer(player)) {
            chat.onSystemChatMessage("Player ${player.name} (ID: ${player.id}) joined.", "#459FFF")
        }

        updateInformation()
        playerList!!.invalidate()
    }

    override fun onPlayerLeft(uid: Long) {

        val player = room!!.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) left.", "#459FFF")
        }

        updateInformation()
        playerList!!.invalidate()
    }

    override fun onPlayerKick(uid: Long) {

        if (uid == player!!.id) {

            Multiplayer.log("Kicked from room.")

            if (GlobalManager.getInstance().engine.scene == GlobalManager.getInstance().gameScene.scene) {
                ToastLogger.showText("You were kicked by the room host, but you can continue playing.", true)
                return
            }

            back()

            mainThread {
                MessageDialog().apply {

                    setTitle("Message")
                    setMessage("You've been kicked by room host.")
                    addButton("Close") { it.dismiss() }

                }.show()
            }
            return
        }

        val player = room!!.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) was kicked.", "#FFBFBF")
        }

        updateInformation()
        playerList!!.invalidate()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus) {

        room!!.playersMap[uid]!!.status = status

        if (uid == player!!.id) {
            isWaitingForStatusChange = false
        }

        updateInformation()
        playerList!!.invalidate()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?) {

        room!!.playersMap[uid]!!.team = team

        playerList!!.invalidate()
        updateInformation()
    }


    fun init() = Unit
}