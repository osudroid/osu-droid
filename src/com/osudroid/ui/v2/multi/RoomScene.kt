package com.osudroid.ui.v2.multi

import com.osudroid.BuildSettings
import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.IPlayerEventListener
import com.osudroid.multiplayer.api.IRoomEventListener
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomPlayer
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.ui.OsuColors
import com.osudroid.ui.v1.SettingsFragment
import com.osudroid.ui.v2.BeatmapInfoLayout
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.UIScene
import com.reco1l.andengine.badge
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.component.plus
import com.reco1l.andengine.component.setText
import com.reco1l.andengine.container
import com.reco1l.andengine.container.JustifyContent
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UIFlexContainer
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.flexContainer
import com.reco1l.andengine.labeledBadge
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.scrollableContainer
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.sprite.ScaleType
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.textButton
import com.reco1l.andengine.ui.SizeVariant
import com.reco1l.andengine.ui.UIBadge
import com.reco1l.andengine.ui.UILabeledBadge
import com.reco1l.andengine.ui.UIMessageDialog
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.toolkt.kotlin.runSafe
import com.rian.osu.mods.ModScoreV2
import org.anddev.andengine.engine.camera.SmoothCamera
import org.anddev.andengine.input.touch.TouchEvent
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class RoomScene(val room: Room) : UIScene(), IRoomEventListener, IPlayerEventListener {

    /**
     * Indicates that the host can change beatmap (it should be false while a change request was done)
     *
     * This is only used if [com.osudroid.multiplayer.Multiplayer.player] is the room host.
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


    private lateinit var backgroundSprite: UISprite
    private lateinit var beatmapInfoLayout: BeatmapInfoLayout
    private lateinit var beatmapInfoAlert: UIText
    private lateinit var modsButton: UITextButton
    private lateinit var startButton: UITextButton
    private lateinit var statusButton: UITextButton
    private lateinit var nameText: UIText
    private lateinit var teamModeBadge: UIBadge
    private lateinit var freeModsBadge: UIBadge
    private lateinit var winConditionBadge: UIBadge
    private lateinit var playersBadge: UILabeledBadge
    private lateinit var modsIndicator: ModsIndicator
    private lateinit var playersContainer: UILinearContainer
    private lateinit var changeBeatmapButton: UITextButton
    private lateinit var downloadBeatmapButton: UITextButton


    private var currentPlayers = LongArray(0)


    init {
        ResourceManager.getInstance().loadHighQualityAsset("mods", "mods.png")
        ResourceManager.getInstance().loadHighQualityAsset("logout", "logout.png")
        ResourceManager.getInstance().loadHighQualityAsset("swap", "swap.png")
        ResourceManager.getInstance().loadHighQualityAsset("clock", "clock.png")
        ResourceManager.getInstance().loadHighQualityAsset("bpm", "bpm.png")
        ResourceManager.getInstance().loadHighQualityAsset("chat", "chat.png")
        ResourceManager.getInstance().loadHighQualityAsset("download", "download.png")
        ResourceManager.getInstance().loadHighQualityAsset("send", "send.png")
        ResourceManager.getInstance().loadHighQualityAsset("settings-icon", "settings-icon.png")
        ResourceManager.getInstance().loadHighQualityAsset("missing", "missing.png")

        RoomAPI.playerEventListener = this
        RoomAPI.roomEventListener = this
        chat = RoomChat()

        container {
            width = FillParent
            height = FillParent
            padding = Vec4(80f, 0f)

            onUpdateTick = {
                if (padding.bottom != chat.buttonHeight) {
                    padding = Vec4(80f, 0f, 80f, chat.buttonHeight + 12f)
                }
            }

            background = UISprite().apply {
                scaleType = ScaleType.Crop
                textureRegion = ResourceManager.getInstance().getTexture("menu-background")
                backgroundSprite = this

                if (!Config.isSafeBeatmapBg()) {
                    textureRegion = ResourceManager.getInstance().getTexture("::background") ?: textureRegion
                }

                foreground = UIBox().apply {
                    applyTheme = {
                        color = it.accentColor * 0.1f
                        alpha = 0.9f
                    }
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                width = FillParent
                height = FillParent

                flexContainer {
                    width = FillParent
                    justifyContent = JustifyContent.SpaceBetween

                    linearContainer {
                        orientation = Orientation.Vertical
                        spacing = 8f
                        padding = Vec4(0f, 18f)

                        nameText = text {
                            text = room.name
                            font = ResourceManager.getInstance().getFont("font")
                            applyTheme = { color = it.accentColor }
                        }

                        linearContainer {
                            spacing = 8f

                            teamModeBadge = badge {
                                sizeVariant = SizeVariant.Small
                            }

                            winConditionBadge = badge {
                                sizeVariant = SizeVariant.Small
                            }

                            playersBadge = labeledBadge {
                                sizeVariant = SizeVariant.Small
                                label = StringTable.get(R.string.multiplayer_room_players)
                                value = "0/${room.maxPlayers}"
                            }

                            freeModsBadge = badge {
                                sizeVariant = SizeVariant.Small
                                applyTheme = {
                                    color = it.accentColor * 0.1f
                                    background?.color = it.accentColor
                                }
                                setText(R.string.multiplayer_room_free_mods)
                                isVisible = false
                            }

                            +ModsIndicator().apply {
                                iconSize = 24f
                                modsIndicator = this
                            }
                        }
                    }

                    textButton {
                        leadingIcon = UISprite(ResourceManager.getInstance().getTexture("settings-icon"))
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        onActionUp = {
                            SettingsFragment().show()
                        }
                        setText(R.string.multiplayer_room_settings)
                    }
                }

                flexContainer {
                    width = FillParent
                    height = FillParent
                    gap = 24f
                    padding = Vec4(0f, 12f)

                    fun UIFlexContainer.Section(title: Int, block: UILinearContainer.() -> Unit) {
                        linearContainer {
                            orientation = Orientation.Vertical
                            height = FillParent
                            spacing = 8f

                            flexRules {
                                grow = 1f
                                basis = 0f
                            }

                            text {
                                text = StringTable.get(title).uppercase()
                                applyTheme = { color = it.accentColor * 0.7f }
                            }

                            block()
                        }
                    }

                    Section(R.string.multiplayer_room_players) {

                        scrollableContainer {
                            width = FillParent
                            relativeSizeAxes = Axes.Y
                            height = 0.75f
                            scrollAxes = Axes.Y
                            clipToBounds = true

                            linearContainer {
                                orientation = Orientation.Vertical
                                width = FillParent
                                spacing = 4f
                                padding = Vec4.One
                                playersContainer = this
                            }
                        }
                    }

                    Section(R.string.multiplayer_room_beatmap) {

                        +BeatmapInfoLayout().apply {
                            padding = Vec4(16f)
                            background = UIBox().apply {
                                cornerRadius = 12f
                                applyTheme = {
                                    color = it.accentColor * 0.1f
                                    alpha = 0.6f
                                }
                            }
                            foreground = UIBox().apply {
                                cornerRadius = 12f
                                paintStyle = PaintStyle.Outline
                                lineWidth = 1f
                                applyTheme = { color = it.accentColor }
                            }
                            isVisible = false
                            beatmapInfoLayout = this
                        }

                        beatmapInfoAlert = text {
                            width = FillParent
                            padding = Vec4(16f)
                            alignment = Anchor.Center
                            background = UIBox().apply {
                                cornerRadius = 12f
                                applyTheme = {
                                    color = it.accentColor * 0.1f
                                    alpha = 0.6f
                                }
                            }
                            foreground = UIBox().apply {
                                cornerRadius = 12f
                                paintStyle = PaintStyle.Outline
                                lineWidth = 1f
                                applyTheme = { color = it.accentColor }
                            }
                        }

                        linearContainer {
                            width = FillParent
                            spacing = 8f

                            changeBeatmapButton = textButton {
                                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("swap"))
                                alignment = Anchor.CenterLeft
                                setText(R.string.multiplayer_room_change_beatmap)
                                onActionUp = {
                                    if (Multiplayer.isRoomHost) {
                                        if (LibraryManager.getLibrary().isEmpty()) {
                                            GlobalManager.getInstance().songService.pause()
                                            BeatmapListing().show()
                                        }

                                        GlobalManager.getInstance().songMenu.reload()
                                        GlobalManager.getInstance().songMenu.show()
                                        GlobalManager.getInstance().songMenu.select()

                                        // We notify all clients that the host is changing beatmap
                                        RoomAPI.changeBeatmap()
                                    }
                                }
                            }

                            downloadBeatmapButton = textButton {
                                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("download"))
                                alignment = Anchor.CenterLeft
                                isVisible = false
                            }
                        }

                    }
                }
            }

            linearContainer {
                origin = Anchor.BottomLeft
                anchor = Anchor.BottomLeft
                spacing = 8f

                textButton {
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("logout"))
                    setText(R.string.multiplayer_room_leave)
                    color = Color4(0xFFFFBFBF)
                    background?.color = Color4(0xFF342121)
                    applyTheme = {}
                    onActionUp = { leaveDialog.show() }
                }

                modsButton = textButton {
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("mods"))
                    setText(R.string.multiplayer_room_mods)
                    onActionUp = { ModMenu.show() }
                }
            }

            linearContainer {
                width = 300f
                orientation = Orientation.Vertical
                origin = Anchor.BottomRight
                anchor = Anchor.BottomRight
                spacing = 8f
                padding = Vec4(0f, 12f)

                textButton {
                    width = FillParent
                    setText(R.string.multiplayer_room_start_game)
                    isSelected = true
                    onActionUp = callback@{

                        if (isWaitingForStatusChange || !Multiplayer.isRoomHost || Multiplayer.player!!.status != PlayerStatus.Ready) {
                            return@callback
                        }

                        if (room.beatmap == null) {
                            ToastLogger.showText(R.string.multiplayer_room_must_select_beatmap, true)
                            return@callback
                        }

                        if (!BuildSettings.MOCK_MULTIPLAYER) {
                            if (room.teamMode == TeamMode.TeamVersus) {
                                val teams = room.teamMap

                                if (teams.values.any { it.isEmpty() }) {
                                    ToastLogger.showText(R.string.multiplayer_room_at_least_one_player_per_team, true)
                                    return@callback
                                }
                            }

                            val players = room.activePlayers.filter { it.status != PlayerStatus.MissingBeatmap }

                            if (players.size <= 1) {
                                ToastLogger.showText(R.string.multiplayer_room_at_least_two_players_beatmap, true)
                                return@callback
                            }
                        }

                        ResourceManager.getInstance().getSound("menuhit")?.play()
                        RoomAPI.notifyMatchPlay()
                    }
                    startButton = this
                }

                textButton {
                    width = FillParent
                    setText(R.string.multiplayer_room_not_ready)
                    onActionUp = callback@{

                        if (isWaitingForStatusChange) {
                            return@callback
                        }

                        ResourceManager.getInstance().getSound("menuclick")?.play()
                        isWaitingForStatusChange = true

                        when (Multiplayer.player!!.status) {

                            PlayerStatus.NotReady -> {
                                if (room.beatmap == null) {
                                    ToastLogger.showText(R.string.multiplayer_room_cannot_ready_changing_beatmap, true)
                                    isWaitingForStatusChange = false
                                    return@callback
                                }

                                if (room.teamMode == TeamMode.TeamVersus && Multiplayer.player!!.team == null) {
                                    ToastLogger.showText(R.string.multiplayer_room_cannot_ready_no_team, true)
                                    isWaitingForStatusChange = false
                                    return@callback
                                }

                                RoomAPI.setPlayerStatus(PlayerStatus.Ready)
                            }

                            PlayerStatus.Ready -> invalidateStatus()

                            PlayerStatus.MissingBeatmap -> {
                                ToastLogger.showText(R.string.multiplayer_room_cannot_ready_missing_beatmap, true)
                                isWaitingForStatusChange = false
                            }

                            else -> isWaitingForStatusChange = false /*This case can never happen, the PLAYING status is set when a game starts*/
                        }
                    }
                    statusButton = this
                }
            }

        }
    }

    // Update events

    private fun updateBackground(path: String?) {
        val textureRegion = ResourceManager.getInstance().loadBackground(path)

        if (textureRegion != null) {
            backgroundSprite.textureRegion = textureRegion
        } else {
            backgroundSprite.textureRegion = ResourceManager.getInstance().getTexture("menu-background")
        }
    }

    private fun updateInformation() {

        updateButtons()

        playersBadge.value = "${room.activePlayers.size}/${room.maxPlayers}"

        winConditionBadge.setText(
            when (room.winCondition) {
                WinCondition.ScoreV1 -> R.string.multiplayer_room_score_v1
                WinCondition.ScoreV2 -> R.string.multiplayer_room_score_v2
                WinCondition.HighestAccuracy -> R.string.multiplayer_room_highest_accuracy
                WinCondition.MaximumCombo -> R.string.multiplayer_room_maximum_combo
            }
        )

        teamModeBadge.setText(
            when (room.teamMode) {
                TeamMode.HeadToHead -> R.string.multiplayer_room_head_to_head
                TeamMode.TeamVersus -> R.string.multiplayer_room_team_versus
            }
        )

        freeModsBadge.isVisible = room.gameplaySettings.isFreeMod

        nameText.text = room.name
        modsIndicator.mods = room.mods.values
    }

    fun updatePlayerList() {

        val shouldReload = currentPlayers.size != room.playersMap.size
            || !currentPlayers.all { room.playersMap.containsKey(it) }

        updateThread {
            playersContainer.apply {

                if (shouldReload) {
                    detachChildren()

                    room.activePlayers.forEach {
                        +RoomPlayerCard().apply {
                            updateState(room, it)
                        }
                    }
                    currentPlayers = room.playersMap.keys.toLongArray()
                } else {
                    room.activePlayers.forEachIndexed { index, player ->
                        val card = getChild(index) as RoomPlayerCard
                        card.updateState(room, player)
                    }
                }
            }
        }
    }

    private fun updateButtons() {
        statusButton.apply {
            setText(
                when (Multiplayer.player!!.status) {
                    PlayerStatus.NotReady -> R.string.multiplayer_room_ready
                    PlayerStatus.Ready -> R.string.multiplayer_room_not_ready
                    else -> R.string.multiplayer_room_unable_status
                }
            )

            isEnabled = when (Multiplayer.player!!.status) {
                PlayerStatus.Ready, PlayerStatus.NotReady -> true
                else -> false
            }

            isSelected = Multiplayer.player!!.status == PlayerStatus.NotReady
        }

        val playersReady = room.activePlayers.filter { it.status == PlayerStatus.Ready }

        startButton.apply {
            isVisible = Multiplayer.isRoomHost && Multiplayer.player!!.status == PlayerStatus.Ready

            // isVisible does only hide the button, but we also need to disable it.
            isEnabled = isVisible

            text = when (playersReady.size) {
                room.activePlayers.size -> StringTable.get(R.string.multiplayer_room_start_game)
                else -> StringTable.format(R.string.multiplayer_room_force_start_game, playersReady.size, room.activePlayers.size)
            }
        }

        modsButton.isEnabled = Multiplayer.isRoomHost || room.gameplaySettings.isFreeMod

        changeBeatmapButton.apply {
            isEnabled = Multiplayer.isRoomHost
            isVisible = Multiplayer.isRoomHost
        }
    }


    private fun updateBeatmap(roomBeatmap: RoomBeatmap?) {

        if (roomBeatmap == null) {
            beatmapInfoLayout.isVisible = false
            beatmapInfoAlert.isVisible = true
            downloadBeatmapButton.apply {
                isVisible = false
                onActionUp = null
            }

            beatmapInfoAlert.setText(
                if (Multiplayer.isRoomHost) {
                    R.string.multiplayer_room_no_beatmap_selected
                } else {
                    R.string.multiplayer_room_changing_beatmap
                }
            )
            return
        }

        beatmapInfoLayout.isVisible = true
        beatmapInfoAlert.isVisible = false

        val beatmapInfo = GlobalManager.getInstance().selectedBeatmap
        beatmapInfoLayout.setBeatmapInfo(beatmapInfo)

        downloadBeatmapButton.apply {

            if (beatmapInfo == null) {
                isVisible = true

                if (roomBeatmap.parentSetID == null) {
                    isEnabled = false
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("download_off"))
                    setText(R.string.multiplayer_room_not_available_beatmap)
                    return@apply
                }

                isEnabled = true
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("download"))
                setText(R.string.multiplayer_room_download_beatmap)
                onActionUp = {
                    val url = BeatmapListing.mirror.download.request(roomBeatmap.parentSetID!!).toString()

                    async {
                        try {
                            BeatmapDownloader.download(url, "${roomBeatmap.parentSetID} ${roomBeatmap.artist} - ${roomBeatmap.title}")
                        } catch (e: Exception) {
                            ToastLogger.showText(StringTable.format(R.string.multiplayer_room_unable_download, e.message), true)
                            e.printStackTrace()
                        }
                    }
                }

            } else {
                isVisible = false
                onActionUp = null
            }
        }
    }

    fun updateBeatmapInfo() {
        beatmapInfoLayout.setBeatmapInfo(GlobalManager.getInstance().selectedBeatmap)
    }


    // Actions

    fun invalidateStatus() {

        // Status shouldn't be changed during reconnection because it's done by server, this function can be called by
        // any of the updating functions.
        if (Multiplayer.isReconnecting) {
            return
        }

        isWaitingForStatusChange = true

        var newStatus = PlayerStatus.NotReady

        if (room.beatmap != null && GlobalManager.getInstance().selectedBeatmap == null) {
            newStatus = PlayerStatus.MissingBeatmap
        }

        if (Multiplayer.player!!.status != newStatus) {
            RoomAPI.setPlayerStatus(newStatus)
        } else {
            isWaitingForStatusChange = false
        }
    }

    // Navigation

    override fun back() {
        Multiplayer.isReconnecting = false

        runSafe { RoomAPI.disconnect() }

        Multiplayer.room = null
        Multiplayer.roomScene = null
        Multiplayer.player = null
        chat.hide()

        UIEngine.current.scene = LobbyScene()
    }

    override fun show() {

        (GlobalManager.getInstance().camera as SmoothCamera).apply {
            setZoomFactorDirect(1f)
            setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        }

        if (!Multiplayer.isConnected) {
            back()
            return
        }

        GlobalManager.getInstance().engine.scene = this

        if (!isWaitingForBeatmapChange) {
            onRoomBeatmapChange(room.beatmap)
        }

        invalidateStatus()
        chat.show()
    }

    override fun onSceneTouchEvent(pSceneTouchEvent: TouchEvent?): Boolean {
        if (super.onSceneTouchEvent(pSceneTouchEvent)) {
            return true
        }

        if (chat.isExpanded) {
            chat.collapse()
        }

        return false
    }


    // Communication

    override fun onServerError(error: String) {
        ToastLogger.showText(error, true)
    }

    override fun onRoomChatMessage(uid: Long?, message: String) {

        if (uid != null) {

            val player = room.playersMap[uid] ?: run {
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

        if (Multiplayer.room != newRoom) {
            chat.onSystemChatMessage(StringTable.get(R.string.multiplayer_room_welcome), "#459FFF")
        }

        ModMenu.clear()
        ModMenu.setMods(newRoom.mods, newRoom.gameplaySettings.isFreeMod)

        isWaitingForModsChange = true

        RoomAPI.setPlayerMods(ModMenu.enabledMods.serializeMods())

        updateInformation()
        updatePlayerList()

        if (Multiplayer.isReconnecting) {
            onRoomBeatmapChange(room.beatmap)

            Multiplayer.onReconnectAttempt(true)

            // If the status returned by server is PLAYING then it means the match was forced to start while the player
            // was disconnected.
            if (Multiplayer.player!!.status == PlayerStatus.Playing) {
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

            chat.onSystemChatMessage(StringTable.get(R.string.multiplayer_room_reconnecting), "#FFBFBF")
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
        room.name = name
        updateInformation()
    }

    override fun onRoomMaxPlayersChange(maxPlayers: Int) {
        room.maxPlayers = maxPlayers
        room.players = room.players.copyOf(maxPlayers)

        updateInformation()
        updatePlayerList()
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?) {

        room.beatmap = beatmap

        GlobalManager.getInstance().selectedBeatmap = LibraryManager.findBeatmapByMD5(beatmap?.md5)

        beatmapInfoLayout.setBeatmapInfo(GlobalManager.getInstance().selectedBeatmap)

        if (GlobalManager.getInstance().engine.scene != this) {
            isWaitingForBeatmapChange = false
            return
        }

        // Notify to the host when other players can't download the beatmap.
        if (Multiplayer.isRoomHost && beatmap != null && beatmap.parentSetID == null) {
            ToastLogger.showText(R.string.multiplayer_room_beatmap_unavailable, false)
        }

        invalidateStatus()

        updateBackground(GlobalManager.getInstance().selectedBeatmap?.backgroundPath)
        updateBeatmap(beatmap)

        isWaitingForBeatmapChange = false

        if (GlobalManager.getInstance().selectedBeatmap == null) {
            GlobalManager.getInstance().songService.stop()
            return
        }

        GlobalManager.getInstance().songService.preLoad(GlobalManager.getInstance().selectedBeatmap!!.audioPath)
        GlobalManager.getInstance().songService.play()
    }

    override fun onRoomHostChange(uid: Long) {

        room.host = uid

        chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_new_host, room.playersMap[uid]?.name.toString()), "#459FFF")

        updateThread {
            ModMenu.back(false)
            ModMenu.updateModButtonVisibility()
            ModMenu.updateCustomizationMenuEnabledStates()
        }

        updatePlayerList()
        updateButtons()
    }


    // Mods

    override fun onRoomModsChange(mods: RoomMods) {

        if (mods != room.mods) {
            invalidateStatus()
        }

        room.mods = mods

        ModMenu.setMods(mods, room.gameplaySettings.isFreeMod)

        isWaitingForModsChange = true

        RoomAPI.setPlayerMods(ModMenu.enabledMods.serializeMods())

        updateInformation()
    }

    override fun onRoomGameplaySettingsChange(settings: RoomGameplaySettings) {
        val wasFreeMod = room.gameplaySettings.isFreeMod

        room.gameplaySettings = settings

        updateButtons()
        updateInformation()
        updatePlayerList()

        isWaitingForModsChange = true

        updateThread {
            ModMenu.back(false)

            if (wasFreeMod != settings.isFreeMod) {
                ModMenu.updateModButtonVisibility()
            }
        }

        invalidateStatus()
    }

    /**This method is used purely to update UI in other clients*/
    override fun onPlayerModsChange(uid: Long, mods: RoomMods) {

        room.playersMap[uid]!!.mods = mods

        updatePlayerList()

        if (uid == Multiplayer.player!!.id) {
            isWaitingForModsChange = false
            updateBeatmapInfo()
        }
    }

    override fun onRoomTeamModeChange(mode: TeamMode) {

        room.teamMode = mode

        updateInformation()
        updatePlayerList()

        isWaitingForStatusChange = true
        invalidateStatus()
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition) {

        room.winCondition = winCondition

        updateInformation()

        if (Multiplayer.isRoomHost) {
            isWaitingForModsChange = true

            // If win condition is Score V2 we add the mod.
            val roomMods = room.mods.apply {

                if (winCondition == WinCondition.ScoreV2)
                    put(ModScoreV2::class)
                else
                    remove(ModScoreV2::class)
            }

            RoomAPI.setRoomMods(roomMods.serializeMods())
        }

        updatePlayerList()
        invalidateStatus()
    }


    // Match

    override fun onRoomMatchPlay() {

        val global = GlobalManager.getInstance()

        if (Multiplayer.player!!.status != PlayerStatus.MissingBeatmap && global.engine.scene != global.gameScene.scene) {

            if (GlobalManager.getInstance().selectedBeatmap == null) {
                Multiplayer.log("WARNING: Attempt to start match with null track.")
                return
            }

            global.songMenu.stopMusic()
            global.gameScene.startGame(global.selectedBeatmap, null, ModMenu.enabledMods)

        }

        updatePlayerList()
    }

    override fun onRoomMatchStart() {

        if (GlobalManager.getInstance().engine.scene is GameLoaderScene) {
            GlobalManager.getInstance().gameScene.isReadyToStart = true
        }

        updatePlayerList()
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
        if (room.addPlayer(player)) {
            chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_player_joined, player.name, player.id), "#459FFF")
        }

        updateInformation()
        updatePlayerList()
    }

    override fun onPlayerLeft(uid: Long) {

        val player = room.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_player_left, player.name, player.id), "#459FFF")
        }

        updateInformation()
        updatePlayerList()
    }

    override fun onPlayerKick(uid: Long) {

        if (uid == Multiplayer.player!!.id) {

            Multiplayer.log("Kicked from room.")

            if (GlobalManager.getInstance().engine.scene == GlobalManager.getInstance().gameScene.scene) {
                ToastLogger.showText(R.string.multiplayer_room_kicked_gameplay, true)
                return
            }

            back()

            UIMessageDialog().apply dialog@{
                title = StringTable.get(R.string.multiplayer_room_kicked_title)
                text = StringTable.get(R.string.multiplayer_room_kicked_message)

                addButton {
                    setText(R.string.multiplayer_room_kicked_close)
                    onActionUp = { this@dialog.hide() }
                }
            }.show()
            return
        }

        val player = room.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_player_kicked, player.name, player.id), "#FFBFBF")
        }

        updateInformation()
        updatePlayerList()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus) {

        room.playersMap[uid]!!.status = status

        if (uid == Multiplayer.player!!.id) {
            isWaitingForStatusChange = false
        }

        updateInformation()
        updatePlayerList()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?) {

        room.playersMap[uid]!!.team = team

        updatePlayerList()
        updateInformation()
    }

}