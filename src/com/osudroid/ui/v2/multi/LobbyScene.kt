package com.osudroid.ui.v2.multi

import android.util.Log
import com.osudroid.multiplayer.*
import com.reco1l.andengine.sprite.*
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.utils.updateThread
import com.osudroid.resources.R.string
import com.osudroid.ui.v2.mainmenu.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import kotlin.coroutines.cancellation.CancellationException
import ru.nsu.ccfit.zuev.osu.helper.StringTable

class LobbyScene : UIScene() {

    private var isFetching = false
    private var shouldFetch = true
    private var lastTimeUpdateMillis: Long = 0L

    private var searchQuery: String? = null
        set(value) {
            if (field != value) {
                field = value
                lastTimeUpdateMillis = System.currentTimeMillis()
                shouldFetch = true
            }
        }

    private lateinit var messageContainer: UILinearContainer
    private lateinit var roomContainer: UILinearContainer
    private lateinit var refreshButton: UITextButton

    private val fetchScope = CoroutineScope(Dispatchers.Default)


    init {
        sprite {
            width = Size.Full
            height = Size.Full
            scaleType = ScaleType.Crop
            textureRegion = ResourceManager.getInstance().getTexture("menu-background")

            if (!Config.isSafeBeatmapBg()) {
                textureRegion = ResourceManager.getInstance().getTexture("::background") ?: textureRegion
            }
        }

        fillContainer {
            orientation = Orientation.Vertical
            width = Size.Full
            height = Size.Full
            style = {
                padding = UIEngine.current.safeArea.copy(y = 2f.srem, w = 2f.srem)
                backgroundColor = it.accentColor * 0.1f / 0.9f
                spacing = 4f.srem
            }

            container {
                width = Size.Full
                style = {
                    padding = Vec4(0f, 4f.srem)
                }

                linearContainer {
                    orientation = Orientation.Horizontal
                    style = {
                        spacing = 2f.srem
                    }

                    textButton {
                        leadingIcon = FontAwesomeIcon(Icon.ArrowLeft)
                        setText(string.multiplayer_lobby_back)
                        onActionUp = { back() }
                    }

                    textButton {
                        leadingIcon = FontAwesomeIcon(Icon.Plus)
                        setText(string.multiplayer_lobby_create_room)
                        onActionUp = {
                            RoomCreateDialog(this@LobbyScene).show()
                        }
                    }

                    refreshButton = textButton {
                        leadingIcon = FontAwesomeIcon(Icon.Rotate)
                        setText(string.multiplayer_lobby_refresh)
                        onActionUp = { shouldFetch = true }
                    }
                }

                container {
                    anchor = Anchor.TopRight
                    origin = Anchor.TopRight
                    height = Size.Full

                    +UITextInput("").apply {
                        key = "search"
                        height = Size.Full
                        placeholder = StringTable.get(ru.nsu.ccfit.zuev.osuplus.R.string.multiplayer_lobby_search_rooms)
                        onValueChange = { value ->
                            searchQuery = value
                        }
                        style = {
                            width = 13f.rem
                        }
                    }

                    container {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = { padding = Vec4(2f.srem, 0f) }
                        +FontAwesomeIcon(Icon.MagnifyingGlass).apply {
                            style = {
                                color = it.accentColor
                            }
                        }
                    }
                }
            }

            container {
                width = Size.Full
                height = Size.Full

                messageContainer = linearContainer {
                    orientation = Orientation.Vertical
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    style = {
                        spacing = 2f.srem
                    }
                }

                scrollableContainer {
                    scrollAxes = Axes.Y
                    width = Size.Full
                    height = Size.Full
                    clipToBounds = true

                    roomContainer = linearContainer {
                        width = Size.Full
                        orientation = Orientation.Vertical
                        scaleCenter = Anchor.Center

                        style = {
                            spacing = 1.5f.srem
                        }
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

        fetchScope.launch(CoroutineExceptionHandler { _, throwable ->
            Log.e("LobbyScene", "Error fetching multiplayer rooms", throwable)

            ToastLogger.showText(
                StringTable.format(string.multiplayer_lobby_fetch_error_toast, throwable.message),
                true
            )

            updateThread {
                messageContainer.apply {
                    detachChildren()

                    text {
                        style = {
                            fontSize = FontSize.SM
                            color = it.accentColor
                        }
                        setText(string.multiplayer_lobby_fetch_error)
                    }
                }
            }

            isFetching = false
        }) {

            messageContainer.apply {
                detachChildren()

                +CircularProgressBar().apply {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    style = {
                        width = 3f.rem
                        height = 3f.rem
                    }
                }
            }

            switchContainers(messageContainer)
            roomContainer.detachChildren()

            val list = LobbyAPI.getRooms(searchQuery, SecurityUtils.signRequest(searchQuery ?: ""))

            updateThread {

                list.forEach {
                    roomContainer.attachChild(RoomButton(this@LobbyScene, it))
                }

                if (list.isEmpty()) {
                    messageContainer.apply {
                        detachChildren()

                        text {
                            style = {
                                fontSize = FontSize.SM
                                color = it.accentColor
                            }
                            setText(if (searchQuery.isNullOrEmpty()) string.multiplayer_lobby_no_rooms else string.multiplayer_lobby_no_results)
                        }
                    }
                } else {
                    switchContainers(roomContainer)
                }

                isFetching = false
            }
        }
    }


    override fun back() {
        Multiplayer.isMultiplayer = false
        GlobalManager.getInstance().songService.isGaming = false
        MainScene.show()
    }


    companion object {
        private const val FETCH_TIMEOUT = 1000L
    }
}