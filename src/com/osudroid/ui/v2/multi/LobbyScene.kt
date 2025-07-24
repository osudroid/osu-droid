package com.osudroid.ui.v2.multi

import android.util.Log
import com.osudroid.multiplayer.*
import com.reco1l.andengine.sprite.*
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.utils.updateThread
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osuplus.*
import kotlin.coroutines.cancellation.CancellationException

class LobbyScene : UIScene() {

    private var isFetching = false
    private var shouldFetch = true
    private var lastTimeUpdateMillis: Long = 0L

    private var search: String? = null
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
                            RoomCreateDialog(this@LobbyScene).show()
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
                            search = value
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

        fetchScope.launch {

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

            val list = LobbyAPI.getRooms(search, SecurityUtils.signRequest(search ?: ""))

            updateThread {

                list.forEach {
                    roomContainer.attachChild(RoomButton(this@LobbyScene, it))
                }

                if (list.isEmpty()) {
                    messageContainer.apply {
                        detachChildren()

                        text {
                            font = ResourceManager.getInstance().getFont("smallFont")
                            applyTheme = { color = it.accentColor }
                            setText(if (search.isNullOrEmpty()) R.string.multiplayer_lobby_no_rooms else R.string.multiplayer_lobby_no_results)
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

        GlobalManager.getInstance().mainScene.show()
    }


    companion object {
        private const val FETCH_TIMEOUT = 1000L
    }
}