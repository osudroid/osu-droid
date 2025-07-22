package com.osudroid.multiplayer

import android.net.Uri
import android.util.Log
import com.reco1l.andengine.sprite.*
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.*
import com.osudroid.utils.updateThread
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.async
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.*
import kotlin.coroutines.cancellation.CancellationException

class LobbyScene : UIScene() {

    /**
     * The search query
     */
    var searchQuery: String? = null
        set(value) {
            if (field != value) {
                field = value
                shouldFetch = true
            }
        }


    private var isFetching = false
    private var shouldFetch = true
    private var lastTimeUpdateMillis: Long = 0L

    private lateinit var messageContainer: UILinearContainer
    private lateinit var roomContainer: UILinearContainer
    private lateinit var refreshButton: UITextButton

    private val fetchScope = CoroutineScope(Dispatchers.Default)


    init {
        ResourceManager.getInstance().loadHighQualityAsset("refresh", "refresh.png")
        ResourceManager.getInstance().loadHighQualityAsset("search-small", "search-small.png")

        linearContainer {
            orientation = Orientation.Vertical
            width = FillParent
            height = FillParent
            padding = Vec4(80f, 0f)

            background = UISprite().apply {
                scaleType = ScaleType.Crop
                textureRegion = ResourceManager.getInstance().getTexture("menu-background")

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

            container {
                width = FillParent
                padding = Vec4(0f, 20f)

                linearContainer {
                    orientation = Orientation.Horizontal
                    spacing = 12f

                    textButton {
                        leadingIcon = UISprite(ResourceManager.getInstance().getTexture("back-arrow"))
                        setText(R.string.multiplayer_lobby_back)
                        onActionUp = { back() }
                    }

                    textButton {
                        leadingIcon = UISprite(ResourceManager.getInstance().getTexture("plus"))
                        setText(R.string.multiplayer_lobby_create_room)
                        onActionUp = {
                            val form: FormContainer

                            object : UIDialog<UIScrollableContainer>(UIScrollableContainer().apply {
                                scrollAxes = Axes.Y
                                width = FillParent
                                height = 360f
                                clipToBounds = true

                                +FormContainer().apply {
                                    form = this
                                    width = FillParent
                                    orientation = Orientation.Vertical

                                    onSubmit = { data ->
                                        async {
                                            LoadingScreen().show()

                                            val name = data.getString("name") ?: StringTable.format(R.string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)
                                            val password = data.optString("password")?.takeUnless(String::isBlank)
                                            val capacity = data.getDouble("capacity").toInt()

                                            val beatmap = GlobalManager.getInstance().selectedBeatmap?.let {

                                                RoomBeatmap(
                                                    md5 = it.md5,
                                                    title = it.title,
                                                    artist = it.artist,
                                                    creator = it.creator,
                                                    version = it.version
                                                )
                                            }

                                            var signStr = "${name}_${capacity}"
                                            if (password != null) {
                                                signStr += "_${password}"
                                            }
                                            signStr += "_${RoomAPI.API_VERSION}"

                                            try {

                                                val roomId = LobbyAPI.createRoom(
                                                    name = name,
                                                    beatmap = beatmap,
                                                    hostUID = OnlineManager.getInstance().userId,
                                                    hostUsername = OnlineManager.getInstance().username,
                                                    sign = SecurityUtils.signRequest(signStr),
                                                    password = password,
                                                    maxPlayers = capacity
                                                )

                                                RoomAPI.connectToRoom(roomId, OnlineManager.getInstance().userId, OnlineManager.getInstance().username, password)

                                            } catch (e: Exception) {
                                                ExtendedEngine.Current.scene = this@LobbyScene
                                                ToastLogger.showText("Failed to create a room: ${e.message}", true)
                                                e.printStackTrace()
                                            }

                                        }
                                    }

                                    +FormInput(StringTable.format(R.string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)).apply {
                                        key = "name"
                                        width = FillParent
                                        label = StringTable.get(R.string.multiplayer_lobby_room_name)
                                    }

                                    +FormInput().apply {
                                        key = "password"
                                        width = FillParent
                                        label = StringTable.get(R.string.multiplayer_lobby_room_password)
                                    }

                                    +FormSlider(8f).apply {
                                        key = "capacity"
                                        width = FillParent
                                        label = StringTable.get(R.string.multiplayer_lobby_room_capacity)
                                        control.max = 16f
                                        control.min = 2f
                                        control.step = 1f
                                        valueFormatter = { it.toInt().toString() }
                                    }

                                }
                            }) {
                                init {
                                    title = StringTable.get(R.string.multiplayer_lobby_create_room)

                                    addButton(UITextButton().apply {
                                        setText(R.string.multiplayer_lobby_create_room_accept)
                                        isSelected = true
                                        onActionUp = {
                                            form.submit()
                                        }
                                    })

                                    addButton(UITextButton().apply {
                                        setText(R.string.multiplayer_lobby_create_room_cancel)
                                        onActionUp = { hide() }
                                    })
                                }
                            }.show()
                        }
                    }

                    refreshButton = textButton {
                        leadingIcon = UISprite(ResourceManager.getInstance().getTexture("refresh"))
                        setText(R.string.multiplayer_lobby_refresh)
                        onActionUp = { shouldFetch = true }
                    }
                }

                container {
                    anchor = Anchor.TopRight
                    origin = Anchor.TopRight
                    height = FillParent

                    +object : UITextInput("") {

                        init {
                            key = "search"
                            width = 500f
                            height = FillParent
                        }

                        override fun onValueChanged() {
                            super.onValueChanged()
                            searchQuery = value
                        }

                    }

                    sprite {
                        textureRegion = ResourceManager.getInstance().getTexture("search-small")
                        size = Vec2(52f, 28f)
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        applyTheme = { color = it.accentColor }
                    }
                }
            }

            container {
                width = FillParent
                height = FillParent

                messageContainer = linearContainer {
                    orientation = Orientation.Vertical
                    spacing = 8f
                    anchor = Anchor.Center
                    origin = Anchor.Center
                }

                scrollableContainer {
                    scrollAxes = Axes.Y
                    width = FillParent
                    height = FillParent
                    clipToBounds = true

                    roomContainer = linearContainer {
                        orientation = Orientation.Vertical
                        spacing = 8f
                        width = FillParent
                        scaleCenter = Anchor.Center
                    }
                }
            }

        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        val currentTimeMillis = System.currentTimeMillis()

        refreshButton.isEnabled = !isFetching && currentTimeMillis - lastTimeUpdateMillis > FETCH_TIMEOUT

        if (shouldFetch && currentTimeMillis - lastTimeUpdateMillis > FETCH_TIMEOUT) {
            shouldFetch = false
            lastTimeUpdateMillis = currentTimeMillis
            fetchRooms()
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    override fun onDetached() {
        fetchScope.cancel(CancellationException())
    }


    private fun switchContainers(target: UIContainer) {

        val opposite = if (target == roomContainer) messageContainer else roomContainer

        opposite.apply {
            clearModifiers(ModifierType.ScaleXY, ModifierType.Alpha)
            scaleTo(0.95f, 0.3f)
            fadeOut(0.3f)
        }

        target.apply {
            clearModifiers(ModifierType.ScaleXY, ModifierType.Alpha)

            scaleX = 0.95f
            scaleY = 0.95f
            alpha = 0f

            scaleTo(1f, 0.3f)
            fadeIn(0.3f)
        }
    }

    private fun fetchRooms() {

        if (isFetching) {
            Log.w("LobbyScene", "Already fetching room list, skipping request.")
            return
        }

        isFetching = true

        messageContainer.apply {
            detachChildren()

            +CircularProgressBar().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                size = Vec2(48f, 48f)
            }
        }

        switchContainers(messageContainer)
        roomContainer.detachChildren()

        val list = LobbyAPI.getRooms(searchQuery, SecurityUtils.signRequest(searchQuery ?: ""))

        updateThread {

            list.forEach {
                roomContainer.attachChild(RoomButton(it))
            }

            if (list.isEmpty()) {
                messageContainer.apply {
                    detachChildren()

                    text {
                        font = ResourceManager.getInstance().getFont("smallFont")
                        applyTheme = { color = it.accentColor }
                        setText(if (searchQuery.isNullOrEmpty()) R.string.multiplayer_lobby_no_rooms else R.string.multiplayer_lobby_no_results)
                    }
                }
            } else {
                switchContainers(roomContainer)
            }

            isFetching = false
        }

    }


    override fun back() {
        Multiplayer.isMultiplayer = false
        GlobalManager.getInstance().songService.isGaming = false

        GlobalManager.getInstance().mainScene.show()
    }


    private inner class RoomButton(val room: Room) : UIButton() {

        init {
            width = FillParent
            background?.apply {
                color = Color4.Black
                alpha = 0.25f
            }

            // Override the default background
            applyTheme = {}

            linearContainer {
                orientation = Orientation.Vertical
                spacing = 8f
                inheritAncestorsColor = false

                linearContainer {
                    orientation = Orientation.Vertical

                    text {
                        font = ResourceManager.getInstance().getFont("smallFont")
                        text = room.name
                        applyTheme = { color = it.accentColor }
                    }

                    text {
                        font = ResourceManager.getInstance().getFont("xs")
                        text = room.playerNames.takeUnless { it.isEmpty() } ?: StringTable.get(R.string.multiplayer_room_no_players)
                        applyTheme = {
                            color = it.accentColor
                            alpha = 0.95f
                        }
                    }
                }

                linearContainer {
                    spacing = 8f

                    badge {
                        sizeVariant = SizeVariant.Small
                        setText(when (room.teamMode) {
                            TeamMode.HeadToHead -> R.string.multiplayer_room_head_to_head
                            TeamMode.TeamVersus  -> R.string.multiplayer_room_team_versus
                        })
                    }

                    badge {
                        sizeVariant = SizeVariant.Small
                        setText(when (room.winCondition) {
                            WinCondition.ScoreV1 -> R.string.multiplayer_room_score_v1
                            WinCondition.ScoreV2 -> R.string.multiplayer_room_score_v2
                            WinCondition.HighestAccuracy -> R.string.multiplayer_room_highest_accuracy
                            WinCondition.MaximumCombo -> R.string.multiplayer_room_maximum_combo
                        })
                    }

                    labeledBadge {
                        sizeVariant = SizeVariant.Small
                        label = StringTable.get(R.string.multiplayer_room_players)
                        value = "${room.players.size}/${room.maxPlayers}"
                    }

                    if (room.gameplaySettings.isFreeMod) {
                        badge {
                            sizeVariant = SizeVariant.Small
                            applyTheme = {
                                color = it.accentColor * 0.1f
                                background?.color = it.accentColor
                            }
                            setText(R.string.multiplayer_room_free_mods)
                        }
                    }

                    if (room.mods.isNotEmpty()) {
                        +ModsIndicator().apply {
                            mods = room.mods.json
                            iconSize = 24f
                        }
                    }
                }
            }

            linearContainer {
                width = FillParent

                if (room.isLocked) {
                    sprite {
                        textureRegion = ResourceManager.getInstance().getTexture("lock")
                        size = Vec2(24f, 24f)
                        anchor = Anchor.TopRight
                        origin = Anchor.TopRight
                    }
                }
            }


            onActionUp = {
                if (room.isLocked) {

                    val form: FormContainer

                    object : UIDialog<FormContainer>(FormContainer().apply {
                        width = FillParent
                        form = this

                        +FormInput().apply {
                            key = "password"
                            width = FillParent
                            label = StringTable.get(R.string.multiplayer_lobby_room_password)
                        }

                        onSubmit = {
                            connectToRoom(it.getString("password"))
                        }
                    }) {
                        init {
                            title = StringTable.get(R.string.multiplayer_lobby_join_room)

                            addButton(UITextButton().apply {
                                setText(R.string.multiplayer_lobby_join_room_accept)
                                isSelected = true
                                onActionUp = { form.submit() }
                            })

                            addButton(UITextButton().apply {
                                setText(R.string.multiplayer_lobby_join_room_cancel)
                                onActionUp = { hide() }
                            })
                        }
                    }.show()
                } else {
                    connectToRoom()
                }
            }
        }


        fun connectToRoom(password: String? = null) {

            Multiplayer.log("Trying to connect socket...")
            LoadingScreen().show()

            async {

                try {
                    RoomAPI.connectToRoom(
                        roomId = room.id,
                        userId = OnlineManager.getInstance().userId,
                        username = OnlineManager.getInstance().username,
                        roomPassword = password
                    )
                } catch (e: Exception) {
                    ToastLogger.showText("Failed to connect to the room: ${e.javaClass} - ${e.message}", true)
                    Multiplayer.log(e)

                    ExtendedEngine.Current.scene = this@LobbyScene
                }
            }

        }

    }


    companion object {
        private const val FETCH_TIMEOUT = 1000L
    }
}