package com.osudroid.multiplayer

import com.osudroid.BuildSettings
import com.osudroid.beatmaplisting.*
import com.reco1l.andengine.sprite.*
import com.osudroid.multiplayer.api.IPlayerEventListener
import com.osudroid.multiplayer.api.IRoomEventListener
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomPlayer
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.TeamMode.HeadToHead
import com.osudroid.multiplayer.api.data.TeamMode.TeamVersus
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.WinCondition.*
import com.osudroid.multiplayer.Multiplayer.isConnected
import com.osudroid.multiplayer.Multiplayer.isRoomHost
import com.osudroid.multiplayer.Multiplayer.player
import com.osudroid.ui.v1.SettingsFragment
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.osu.ui.entity.ComposedText
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.multi.*
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.ui.v2.multi.RoomChat
import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.runSafe
import com.rian.osu.mods.ModScoreV2
import org.anddev.andengine.engine.camera.SmoothCamera
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.*

class RoomScene(val room: Room) : UIScene(), IRoomEventListener, IPlayerEventListener {

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


    init {
        ResourceManager.getInstance().loadHighQualityAsset("mods", "mods.png")
        ResourceManager.getInstance().loadHighQualityAsset("logout", "logout.png")
        ResourceManager.getInstance().loadHighQualityAsset("swap", "swap.png")
        ResourceManager.getInstance().loadHighQualityAsset("clock", "clock.png")
        ResourceManager.getInstance().loadHighQualityAsset("bpm", "bpm.png")
        ResourceManager.getInstance().loadHighQualityAsset("star", "star.png")
        ResourceManager.getInstance().loadHighQualityAsset("chat", "chat.png")
        ResourceManager.getInstance().loadHighQualityAsset("download", "download.png")
        ResourceManager.getInstance().loadHighQualityAsset("send", "send.png")
        ResourceManager.getInstance().loadHighQualityAsset("settings-icon", "settings-icon.png")

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
                        text = "Settings"
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        onActionUp = {
                            SettingsFragment().show()
                        }
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
                            height = FillParent
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
                                    alpha = 0.5f
                                }
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
                                    alpha = 0.5f
                                }
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
                                    if (isRoomHost) {
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
                padding = Vec4(0f, 12f)

                textButton {
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("logout"))
                    setText(R.string.multiplayer_room_leave)
                    onActionUp = { back() }
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

                        if (isWaitingForStatusChange || !isRoomHost || player!!.status != Ready) {
                            return@callback
                        }

                        if (room.beatmap == null) {
                            ToastLogger.showText(R.string.multiplayer_room_must_select_beatmap, true)
                            return@callback
                        }

                        if (!BuildSettings.MOCK_MULTIPLAYER) {
                            if (room.teamMode == TeamVersus) {
                                val teams = room.teamMap

                                if (teams.values.any { it.isEmpty() }) {
                                    ToastLogger.showText(R.string.multiplayer_room_at_least_one_player_per_team, true)
                                    return@callback
                                }
                            }

                            val players = room.activePlayers.filter { it.status != MissingBeatmap }

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

                        when (player!!.status) {

                            NotReady -> {
                                if (room.beatmap == null) {
                                    ToastLogger.showText("Cannot ready when the host is changing beatmap.", true)
                                    isWaitingForStatusChange = false
                                }

                                if (room.teamMode == TeamVersus && player!!.team == null) {
                                    ToastLogger.showText("You must select a team first!", true)
                                    isWaitingForStatusChange = false
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
                    }
                    statusButton = this
                }
            }

        }

        Multiplayer.roomScene = this
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
                ScoreV1 -> R.string.multiplayer_room_score_v1
                ScoreV2 -> R.string.multiplayer_room_score_v2
                HighestAccuracy -> R.string.multiplayer_room_highest_accuracy
                MaximumCombo -> R.string.multiplayer_room_maximum_combo
            }
        )

        teamModeBadge.setText(
            when (room.teamMode) {
                HeadToHead -> R.string.multiplayer_room_head_to_head
                TeamVersus -> R.string.multiplayer_room_team_versus
            }
        )

        freeModsBadge.isVisible = room.gameplaySettings.isFreeMod

        nameText.text = room.name
        modsIndicator.mods = room.mods.json
    }

    private fun updatePlayerList() {
        updateThread {
            playersContainer.apply {
                detachChildren()

                room.players.filterNotNull().forEach {
                    +RoomPlayerButton(room, it)
                }
            }
        }
    }

    private fun updateButtons() {

        statusButton.apply {
            setText(
                when (player!!.status) {
                    NotReady -> R.string.multiplayer_room_ready
                    Ready -> R.string.multiplayer_room_not_ready
                    else -> R.string.multiplayer_room_unable_status
                }
            )

            isEnabled = when (player!!.status) {
                Ready, NotReady -> true
                else -> false
            }

            isSelected = player!!.status == NotReady
        }

        val playersReady = room.activePlayers.filter { it.status == Ready }

        startButton.apply {
            isVisible = isRoomHost && player!!.status == Ready

            // isVisible does only hide the button, but we also need to disable it.
            isEnabled = isVisible

            text = when (playersReady.size) {
                room.activePlayers.size -> StringTable.get(R.string.multiplayer_room_start_game)
                else -> StringTable.format(R.string.multiplayer_room_force_start_game, playersReady.size, room.activePlayers.size)
            }
        }

        modsButton.isEnabled = isRoomHost || room.gameplaySettings.isFreeMod

        changeBeatmapButton.apply {
            isEnabled = isRoomHost
            isVisible = isRoomHost
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
                if (isRoomHost) {
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
                            ToastLogger.showText("Unable to download beatmap: ${e.message}", true)
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

        var newStatus = NotReady

        if (room.beatmap != null && GlobalManager.getInstance().selectedBeatmap == null) {
            newStatus = MissingBeatmap
        }

        if (player!!.status != newStatus) {
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
        player = null
        chat.hide()

        UIEngine.current.scene = LobbyScene()
    }

    override fun show() {

        (GlobalManager.getInstance().camera as SmoothCamera).apply {
            setZoomFactorDirect(1f)
            setCenterDirect(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f)
        }

        if (!isConnected) {
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
            chat.onSystemChatMessage("Welcome to osu!droid multiplayer", "#459FFF")
        }

        Multiplayer.room = newRoom
        player = newRoom.playersMap[OnlineManager.getInstance().userId]!!

        clearChildScene()

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
        if (isRoomHost && beatmap != null && beatmap.parentSetID == null) {
            ToastLogger.showText("This beatmap isn't available in the download mirror servers.", false)
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

        chat.onSystemChatMessage("Player ${room.playersMap[uid]?.name} is now the room host.", "#459FFF")

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

        if (uid == player!!.id) {
            isWaitingForModsChange = false
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

        if (isRoomHost) {
            isWaitingForModsChange = true

            // If win condition is Score V2 we add the mod.
            val roomMods = room.mods.apply {

                if (winCondition == ScoreV2)
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

        if (player!!.status != MissingBeatmap && global.engine.scene != global.gameScene.scene) {

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
            chat.onSystemChatMessage("Player ${player.name} (ID: ${player.id}) joined.", "#459FFF")
        }

        updateInformation()
        updatePlayerList()
    }

    override fun onPlayerLeft(uid: Long) {

        val player = room.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) left.", "#459FFF")
        }

        updateInformation()
        updatePlayerList()
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

        val player = room.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage("Player ${player.name} (ID: $uid) was kicked.", "#FFBFBF")
        }

        updateInformation()
        updatePlayerList()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus) {

        room.playersMap[uid]!!.status = status

        if (uid == player!!.id) {
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