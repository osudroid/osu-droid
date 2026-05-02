package com.osudroid.ui.v2.multi

import com.osudroid.BuildSettings
import java.util.concurrent.atomic.AtomicBoolean
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
import com.osudroid.ui.v1.SettingsFragment
import com.osudroid.ui.v2.BeatmapInfoLayout
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.osudroid.utils.updateThread
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.UIScene
import com.reco1l.andengine.badge
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.component.forEach
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
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class RoomScene(
    /**
     * The [Room] this [RoomScene] is showing. Updated in-place when the socket reconnects so that this [RoomScene] can
     * be reused and update its current state accordingly.
     */
    var room: Room
) : UIScene(), IRoomEventListener, IPlayerEventListener {
    /**
     * Indicates that the host can change beatmap. **This must be `false` when waiting for a beatmap change request**.
     *
     * This is only used if [com.osudroid.multiplayer.Multiplayer.player] is the room host.
     */
    @JvmField
    val isWaitingForBeatmapChange = AtomicBoolean(false)

    /**
     * Indicates that the player can change its status. Its purpose is to await for server changes.
     */
    @JvmField
    val isWaitingForStatusChange = AtomicBoolean(false)

    /**
     * Indicates that the player can change its mods. Its purpose is to await for server changes.
     */
    @JvmField
    val isWaitingForModsChange = AtomicBoolean(false)

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

                        if (isWaitingForStatusChange.get() || !Multiplayer.isRoomHost || Multiplayer.player?.status != PlayerStatus.Ready) {
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

                        // Atomic guard: only the first tap wins even if two ACTION_UP events
                        // are delivered before the server reply clears the flag.
                        if (!isWaitingForStatusChange.compareAndSet(false, true)) {
                            return@callback
                        }

                        ResourceManager.getInstance().getSound("menuclick")?.play()

                        // Guard: if back() nulled player between the isWaitingForStatusChange check
                        // and here, reset the flag and exit instead of crashing.
                        val player = Multiplayer.player ?: run {
                            isWaitingForStatusChange.set(false)
                            return@callback
                        }

                        when (player.status) {

                            PlayerStatus.NotReady -> {
                                if (room.beatmap == null) {
                                    ToastLogger.showText(R.string.multiplayer_room_cannot_ready_changing_beatmap, true)
                                    isWaitingForStatusChange.set(false)
                                    return@callback
                                }

                                if (room.teamMode == TeamMode.TeamVersus && player.team == null) {
                                    ToastLogger.showText(R.string.multiplayer_room_cannot_ready_no_team, true)
                                    isWaitingForStatusChange.set(false)
                                    return@callback
                                }

                                RoomAPI.setPlayerStatus(PlayerStatus.Ready)
                            }

                            PlayerStatus.Ready -> invalidateStatus()

                            PlayerStatus.MissingBeatmap -> {
                                ToastLogger.showText(R.string.multiplayer_room_cannot_ready_missing_beatmap, true)
                                isWaitingForStatusChange.set(false)
                            }

                            else -> isWaitingForStatusChange.set(false) /*This case can never happen, the PLAYING status is set when a game starts*/
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

    fun updatePlayerList() = updateThread {
        val shouldReload = currentPlayers.size != room.playersMap.size
                || !currentPlayers.all { room.playersMap.containsKey(it) }

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

    private fun updateButtons() {
        // Guard: player is nulled by back() when the scene is tearing down; any
        // concurrent call (socket EventThread or update thread) must exit cleanly.
        val player = Multiplayer.player ?: return

        statusButton.apply {
            setText(
                when (player.status) {
                    PlayerStatus.NotReady -> R.string.multiplayer_room_ready
                    PlayerStatus.Ready -> R.string.multiplayer_room_not_ready
                    else -> R.string.multiplayer_room_unable_status
                }
            )

            isEnabled = when (player.status) {
                PlayerStatus.Ready, PlayerStatus.NotReady -> true
                else -> false
            }

            isSelected = player.status == PlayerStatus.NotReady
        }

        val playersReady = room.activePlayers.filter { it.status == PlayerStatus.Ready }

        startButton.apply {
            isVisible = Multiplayer.isRoomHost && player.status == PlayerStatus.Ready

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

        updateBeatmapInfo(roomBeatmap)

        downloadBeatmapButton.apply {
            val beatmapInfo = GlobalManager.getInstance().selectedBeatmap

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
                    val url = BeatmapListing.mirror.download.request(
                        roomBeatmap.parentSetID!!,
                        !Config.isPreferNoVideoDownloads()
                    ).toString()

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

    @JvmOverloads
    fun updateBeatmapInfo(roomBeatmap: RoomBeatmap? = room.beatmap) {
        val beatmapInfo = GlobalManager.getInstance().selectedBeatmap

        if (beatmapInfo != null) {
            beatmapInfoLayout.setBeatmapInfo(beatmapInfo)
        } else {
            beatmapInfoLayout.setBeatmapInfo(roomBeatmap)
        }
    }


    // Actions

    fun invalidateStatus() {

        // Status shouldn't be changed during reconnection because it's done by server, this function can be called by
        // any of the updating functions.
        if (Multiplayer.isReconnecting) {
            return
        }

        // Guard: player is nulled by back(); if we race past the reconnecting check, exit cleanly.
        val player = Multiplayer.player ?: return

        isWaitingForStatusChange.set(true)

        var newStatus = PlayerStatus.NotReady

        if (room.beatmap != null && GlobalManager.getInstance().selectedBeatmap == null) {
            newStatus = PlayerStatus.MissingBeatmap
        }

        if (player.status != newStatus) {
            RoomAPI.setPlayerStatus(newStatus)
        } else {
            isWaitingForStatusChange.set(false)
        }
    }

    // Navigation

    /**
     * Tears down all multiplayer state: cancels reconnection, nulls event listeners,
     * disconnects the socket, cancels pending jobs, and hides the chat.
     *
     * This is the common teardown path shared by [back] (which also navigates to the lobby)
     * and the kicked-during-game handler (which must NOT navigate since the game scene must
     * be allowed to finish).
     *
     * **Must be called on the update thread.**
     */
    private fun teardownSession() {
        Multiplayer.cancelReconnection()
        beatmapInfoLayout.cancelCalculation()
        playersContainer.forEach { (it as RoomPlayerCard).cancelJobs() }

        // Null out event listeners before disconnect so any queued socket events that
        // arrive after teardown find no listener to call.
        RoomAPI.roomEventListener = null
        RoomAPI.playerEventListener = null

        runSafe { RoomAPI.disconnect() }

        Multiplayer.room = null
        Multiplayer.roomScene = null
        Multiplayer.player = null
        chat.hide()
    }

    override fun back() {
        teardownSession()
        UIEngine.current.scene = LobbyScene()
    }

    override fun show() {
        if (!Multiplayer.isConnected) {
            back()
            return
        }

        GlobalManager.getInstance().engine.scene = this

        if (!isWaitingForBeatmapChange.get()) {
            // onRoomBeatmapChange sets engine.scene = this just above, so it will always
            // find the scene active and call invalidateStatus() internally.  Do NOT call
            // invalidateStatus() again afterwards — that would emit a redundant
            // setPlayerStatus to the server (SI-4).
            onRoomBeatmapChange(room.beatmap)
        }
        // When isWaitingForBeatmapChange == true a beatmap change is already in flight.
        // Emitting a status now would race the imminent onRoomBeatmapChange callback that
        // will call invalidateStatus() with the correct beatmap context.

        chat.show()
    }


    // Communication

    override fun onServerError(error: String) {
        mainThread { ToastLogger.showText(error, true) }
    }

    override fun onRoomChatMessage(uid: Long?, message: String) {

        if (uid != null) {
            // Look up the sender. If they are not yet in playersMap the most likely cause is
            // that chatMessage arrived on the EventThread just before the playerJoined event
            // that was queued right behind it (EH-3). Rather than silently discarding the
            // message, build a temporary stub player so the text is displayed in the chat log.
            // The stub uses "#uid" as its name; once playerJoined fires, any subsequent
            // messages from that player will show their real username.
            val player = room.playersMap[uid] ?: run {
                Multiplayer.log("WARNING: chatMessage from unknown UID $uid — displaying with stub name")
                RoomPlayer(id = uid, name = "#$uid", status = PlayerStatus.NotReady, team = null, mods = RoomMods())
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

        isWaitingForModsChange.set(true)

        RoomAPI.setPlayerMods(ModMenu.enabledMods.serializeMods())

        updateInformation()
        updatePlayerList()

        if (Multiplayer.isReconnecting) {
            onRoomBeatmapChange(room.beatmap)

            Multiplayer.onReconnectAttempt(true)

            // If the status returned by server is PLAYING then it means the match was forced to start while the player
            // was disconnected.
            val player = Multiplayer.player ?: return
            if (player.status == PlayerStatus.Playing) {
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

        val beatmapInfo = LibraryManager.findBeatmapByMD5(newRoom.beatmap?.md5)
        GlobalManager.getInstance().selectedBeatmap = beatmapInfo

        updateBackground(beatmapInfo?.backgroundPath)
        updateBeatmap(newRoom.beatmap)

        show()
    }

    override fun onRoomDisconnect(reason: String?, byUser: Boolean) {

        if (!byUser) {
            // Setting await locks to avoid player emitting events that will be ignored.
            isWaitingForBeatmapChange.set(true)
            isWaitingForStatusChange.set(true)
            isWaitingForModsChange.set(true)

            chat.onSystemChatMessage(StringTable.get(R.string.multiplayer_room_reconnecting), "#FFBFBF")
            Multiplayer.onReconnect()
            return
        }

        updateThread { back() }
    }

    override fun onRoomConnectFail(error: String?) {
        Multiplayer.log("ERROR: Failed to connect -> $error")

        if (Multiplayer.isReconnecting) {
            Multiplayer.onReconnectAttempt(false)
            return
        }

        updateThread { back() }
    }


    // Room changes

    override fun onRoomNameChange(name: String) {
        room.name = name
        updateThread { updateInformation() }
    }

    override fun onRoomMaxPlayersChange(maxPlayers: Int) {
        // The server caps maxPlayers to the current amount of players in the lobby.
        // If the server sends back the current maxPlayers value, we don't need to update anything.
        if (maxPlayers == room.maxPlayers) {
            return
        }

        room.maxPlayers = maxPlayers
        room.resizePlayers(maxPlayers)

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onRoomBeatmapChange(beatmap: RoomBeatmap?) {

        room.beatmap = beatmap

        GlobalManager.getInstance().selectedBeatmap = LibraryManager.findBeatmapByMD5(beatmap?.md5)

        if (GlobalManager.getInstance().engine.scene != this) {
            updateThread { updateBeatmapInfo() }
            isWaitingForBeatmapChange.set(false)
            return
        }

        // Notify to the host when other players can't download the beatmap.
        if (Multiplayer.isRoomHost && beatmap != null && beatmap.parentSetID == null) {
            mainThread { ToastLogger.showText(R.string.multiplayer_room_beatmap_unavailable, false) }
        }

        invalidateStatus()
        isWaitingForBeatmapChange.set(false)

        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap
        updateThread {
            updateBackground(selectedBeatmap?.backgroundPath)
            updateBeatmap(beatmap)
        }

        if (selectedBeatmap == null) {
            GlobalManager.getInstance().songService.stop()
            return
        }

        GlobalManager.getInstance().songService.preLoad(selectedBeatmap.audioPath)
        GlobalManager.getInstance().songService.play()
    }

    override fun onRoomHostChange(uid: Long) {

        room.host = uid

        val newHostName = room.playersMap[uid]?.name ?: "#$uid"
        chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_new_host, newHostName), "#459FFF")

        updateThread {
            ModMenu.back(false)
            ModMenu.updateModButtonVisibility()
            ModMenu.updateCustomizationMenuEnabledStates()
            updateButtons()
        }

        updatePlayerList()
    }


    // Mods

    override fun onRoomModsChange(mods: RoomMods) {

        if (!mods.equalsWithContext(room.mods, room.gameplaySettings.isFreeMod)) {
            invalidateStatus()
        }

        room.mods = mods

        isWaitingForModsChange.set(true)

        updateThread {
            // Apply the new room mods to ModMenu first so that enabledMods is up-to-date
            // before we serialize and emit to the server.  Serializing before setMods() runs
            // would send the previous (stale) mods payload to the server.
            ModMenu.setMods(mods, room.gameplaySettings.isFreeMod)
            RoomAPI.setPlayerMods(ModMenu.enabledMods.serializeMods())
            updateInformation()
        }
    }

    override fun onRoomGameplaySettingsChange(settings: RoomGameplaySettings) {
        val wasFreeMod = room.gameplaySettings.isFreeMod

        room.gameplaySettings = settings

        updatePlayerList()

        isWaitingForModsChange.set(true)

        updateThread {
            updateButtons()
            updateInformation()
            ModMenu.back(false)

            if (wasFreeMod != settings.isFreeMod) {
                ModMenu.updateModButtonVisibility()
            }
        }

        invalidateStatus()
    }

    /**This method is used purely to update UI in other clients*/
    override fun onPlayerModsChange(uid: Long, mods: RoomMods) {

        val target = room.playersMap[uid] ?: run {
            Multiplayer.log("WARNING: onPlayerModsChange — unknown uid $uid, player may have already left")
            return
        }
        target.mods = mods

        updatePlayerList()

        val player = Multiplayer.player ?: return
        if (uid == player.id) {
            isWaitingForModsChange.set(false)
            updateThread { updateBeatmapInfo() }
        }
    }

    override fun onRoomTeamModeChange(mode: TeamMode) {

        room.teamMode = mode

        updateThread { updateInformation() }
        updatePlayerList()

        isWaitingForStatusChange.set(true)
        invalidateStatus()
    }

    override fun onRoomWinConditionChange(winCondition: WinCondition) {

        room.winCondition = winCondition

        updateThread { updateInformation() }

        if (Multiplayer.isRoomHost) {
            isWaitingForModsChange.set(true)

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

        val player = Multiplayer.player ?: return

        if (player.status == PlayerStatus.MissingBeatmap) {
            // This client does not have the beatmap and will never call startGame(), so
            // GameScene.onGameLoad() will never fire and notifyBeatmapLoaded() would never be
            // sent.  The server waits for every client's beatmapLoadComplete before sending
            // allPlayersBeatmapLoadComplete, so one missing-beatmap client would block the
            // match for everyone (EH-5).  Send the ACK immediately so the server can proceed
            // once all other players have loaded.
            Multiplayer.log("INFO: MissingBeatmap — sending immediate beatmapLoadComplete ACK (EH-5)")
            RoomAPI.notifyBeatmapLoaded()
        }

        updateThread {
            val global = GlobalManager.getInstance()
            if (player.status != PlayerStatus.MissingBeatmap && global.engine.scene != global.gameScene.scene) {

                if (global.selectedBeatmap == null) {
                    Multiplayer.log("WARNING: Attempt to start match with null track.")
                    return@updateThread
                }

                global.songMenu.stopMusic()
                global.gameScene.startGame(global.selectedBeatmap, null, ModMenu.enabledMods)

            }
        }

        updatePlayerList()
    }

    override fun onRoomMatchStart() {
        updateThread {
            if (GlobalManager.getInstance().engine.scene is GameLoaderScene) {
                GlobalManager.getInstance().gameScene.isReadyToStart = true
            }
        }

        updatePlayerList()
    }

    override fun onRoomMatchSkip() {

        updateThread {
            if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
                return@updateThread
            }

            GlobalManager.getInstance().gameScene.skip()
        }
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

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerLeft(uid: Long) {

        val player = room.removePlayer(uid)

        if (player != null) {
            chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_player_left, player.name, player.id), "#459FFF")
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerKick(uid: Long) {

        val player = Multiplayer.player ?: return

        if (uid == player.id) {

            Multiplayer.log("Kicked from room.")

            updateThread {
                if (GlobalManager.getInstance().engine.scene == GlobalManager.getInstance().gameScene.scene) {
                    // Kicked while a game is in progress. We cannot navigate away from the
                    // game scene immediately — doing so would abruptly interrupt gameplay.
                    // Instead:
                    //   1. Show the toast so the player knows why they were kicked.
                    //   2. Tear down all multiplayer state (disconnect socket, null globals,
                    //      cancel coroutines, hide chat) so live-score events stop firing.
                    //   3. Clear isMultiplayer so the scoring scene and GameScene treat the
                    //      remainder of the game as a solo session — submitFinalScore() will
                    //      not be called, and ScoringScene.back() will return to SongMenu
                    //      rather than trying to re-enter the (now-disconnected) room.
                    ToastLogger.showText(R.string.multiplayer_room_kicked_gameplay, true)
                    teardownSession()
                    Multiplayer.isMultiplayer = false

                    return@updateThread
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
            }
            return
        }

        val removedPlayer = room.removePlayer(uid)

        if (removedPlayer != null) {
            chat.onSystemChatMessage(StringTable.format(R.string.multiplayer_room_player_kicked, removedPlayer.name, removedPlayer.id), "#FFBFBF")
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerStatusChange(uid: Long, status: PlayerStatus) {

        val target = room.playersMap[uid] ?: run {
            Multiplayer.log("WARNING: onPlayerStatusChange — unknown uid $uid, player may have already left")
            return
        }
        target.status = status

        val player = Multiplayer.player
        if (player != null && uid == player.id) {
            isWaitingForStatusChange.set(false)
        }

        updateThread { updateInformation() }
        updatePlayerList()
    }

    override fun onPlayerTeamChange(uid: Long, team: RoomTeam?) {

        val target = room.playersMap[uid] ?: run {
            Multiplayer.log("WARNING: onPlayerTeamChange — unknown uid $uid, player may have already left")
            return
        }
        target.team = team

        updatePlayerList()
        updateThread { updateInformation() }
    }

}