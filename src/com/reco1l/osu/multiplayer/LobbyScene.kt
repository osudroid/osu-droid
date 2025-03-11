package com.reco1l.osu.multiplayer

import android.net.Uri
import android.util.Log
import com.reco1l.andengine.sprite.*
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import com.reco1l.ibancho.LobbyAPI
import com.reco1l.ibancho.RoomAPI
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.async
import org.anddev.andengine.entity.modifier.LoopEntityModifier
import org.anddev.andengine.entity.modifier.RotationByModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.scene.background.SpriteBackground
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.skins.OsuSkin

object LobbyScene : Scene() {

    /**
     * The search fragment
     */
    val search by lazy { LobbySearch() }

    /**
     * The search query
     */
    var searchQuery: String? = null
        set(value) {
            field = value

            if (!isWaitingForList) {
                updateList()
            }
        }


    private var backButton: ExtendedSprite? = null

    private var createButton: TextButton? = null

    private var refreshButton: TextButton? = null


    private val roomsList = LobbyRoomList()

    private val userCard = OnlinePanel()

    private val titleText = ChangeableText(20f, 20f, ResourceManager.getInstance().getFont("bigFont"), "", 100)

    private val informationText = ChangeableText(20f, 0f, ResourceManager.getInstance().getFont("smallFont"), "", 100)

    private val loadingIcon = Sprite(0f, 0f, ResourceManager.getInstance().getTexture("loading_start"))


    /**
     * Whether the scene is waiting for the room list to be loaded.
     */
    private var isWaitingForList = false


    fun load() {
        detachChildren()
        clearTouchAreas()

        isBackgroundEnabled = true
        updateBackground()

        val dimRectangle = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        dimRectangle.setColor(0f, 0f, 0f, 0.5f)
        attachChild(dimRectangle)

        val topBarRectangle = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 120f)
        topBarRectangle.setColor(0f, 0f, 0f, 0.3f)
        attachChild(topBarRectangle)

        titleText.text = "Multiplayer Lobby"
        attachChild(titleText)

        informationText.setPosition(20f, titleText.y + titleText.height)
        informationText.setColor(0.8f, 0.8f, 0.8f)
        attachChild(informationText)

        attachChild(roomsList, 1)

        loadingIcon.setPosition((Config.getRES_WIDTH() - loadingIcon.width) / 2f, (Config.getRES_HEIGHT() - loadingIcon.height) / 2f)
        loadingIcon.setRotationCenter(loadingIcon.width / 2f, loadingIcon.height / 2f)
        loadingIcon.setScale(0.4f)
        attachChild(loadingIcon)

        // Back button code copy and paste from legacy code but improved, don't blame on me.
        val layoutBackButton = OsuSkin.get().getLayout("BackButton")

        backButton = object : AnimatedSprite("menu-back", true, OsuSkin.get().animationFramerate) {

            var scaleWhenHold = layoutBackButton?.property?.optBoolean("scaleWhenHold", true) ?: false
            var moved = false
            var dx = 0f
            var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                if (event.isActionDown) {
                    if (scaleWhenHold) this.setScale(1.25f)

                    moved = false
                    dx = localX
                    dy = localY

                    ResourceManager.getInstance().getSound("menuback")?.play()
                    return true
                }

                if (event.isActionUp) {
                    this.setScale(1f)

                    if (!moved) back()
                    return true
                }

                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 50) {
                    this.setScale(1f)
                    moved = true
                }
                return false
            }
        }.also {

            if (OsuSkin.get().isUseNewLayout) {
                layoutBackButton?.apply(it) ?: it.setPosition(0f, Config.getRES_HEIGHT() - it.heightScaled)
            } else {
                it.setPosition(0f, Config.getRES_HEIGHT() - it.heightScaled)
            }

            attachChild(it)
            registerTouchArea(it)
        }

        userCard.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
        attachChild(userCard)

        createButton = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"), "Create New Room") {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (!event.isActionUp || isWaitingForList) return false

                ResourceManager.getInstance().getSound("menuclick")?.play()
                LobbyCreateRoom().show()
                return true
            }
        }.also {

            it.width = 400f
            it.setColor(0.2f, 0.2f, 1f, 0.9f)
            it.setPosition(40f, 120f + 40f)
            attachChild(it)
            registerTouchArea(it)
        }

        refreshButton = object : TextButton(ResourceManager.getInstance().getFont("CaptionFont"), "Refresh") {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (!event.isActionUp || isWaitingForList) return false

                ResourceManager.getInstance().getSound("menuclick")?.play()
                updateList()
                return true
            }
        }.also {

            it.width = 400f
            it.setColor(0.2f, 0.2f, 0.2f, 0.9f)
            it.setPosition(40f, createButton!!.y + createButton!!.height + 30f)
            attachChild(it)
            registerTouchArea(it)
        }
    }


    fun connectFromLink(link: Uri) {

        if (Multiplayer.isConnected) {
            return
        }

        GlobalManager.getInstance().songService.isGaming = true
        Multiplayer.isMultiplayer = true

        async {

            try {
                LoadingScreen().show()

                GlobalManager.getInstance().mainActivity.checkNewSkins()
                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                RoomScene.load()
                load()

                val roomID = link.pathSegments[0].toLong()
                val password = if (link.pathSegments.size > 1) link.pathSegments[1] else null

                RoomAPI.connectToRoom(
                    roomId = roomID,
                    userId = OnlineManager.getInstance().userId,
                    username = OnlineManager.getInstance().username,
                    roomPassword = password
                )

            } catch (e: Exception) {
                ToastLogger.showText("Failed to connect to the room: ${e.javaClass} - ${e.message}", true)
                Log.e("LobbyScene", "Failed to connect to room.", e)

                back()
            }


        }

    }


    @JvmStatic
    fun updateOnlinePanel() = updateThread {

        userCard.setInfo()
        userCard.setAvatar()
    }

    fun updateList() {

        createButton?.isVisible = false
        refreshButton?.isVisible = false

        informationText.text = "Loading room list..."

        roomsList.detachChildren()

        loadingIcon.isVisible = true
        loadingIcon.registerEntityModifier(LoopEntityModifier(RotationByModifier(2f, 360f)))

        isWaitingForList = true

        async {

            try {
                val list = LobbyAPI.getRooms(searchQuery, SecurityUtils.signRequest(searchQuery ?: ""))

                updateThread {
                    roomsList.setList(list)
                    isWaitingForList = false
                    val roomCount = roomsList.childCount

                    informationText.text = if (searchQuery.isNullOrEmpty()) {
                        "Showing $roomCount ${if (roomCount == 1) "match" else "matches"}."
                    } else {
                        "Searching for \"$searchQuery\", showing $roomCount ${if (roomCount == 1) "result" else "results"}."
                    }
                }

                loadingIcon.isVisible = false
                loadingIcon.clearEntityModifiers()

                // Showing buttons
                createButton?.isVisible = true
                refreshButton?.isVisible = true

            } catch (e: Exception) {
                isWaitingForList = false

                ToastLogger.showText("Multiplayer server is unavailable, try again later.", true)
                Log.e("LobbyScene", "Failed to get room list", e)

                back()
            }

        }

    }


    private fun updateBackground() {

        var texture = ResourceManager.getInstance().getTexture("menu-background")

        if (!Config.isSafeBeatmapBg()) {
            texture = ResourceManager.getInstance().getTexture("::background") ?: texture
        }

        texture?.also {

            val height = it.height * (Config.getRES_WIDTH() / it.width.toFloat())
            val width = Config.getRES_WIDTH().toFloat()

            background = SpriteBackground(Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, width, height, it))
        }
    }


    override fun back() {

        if (isWaitingForList) {
            return
        }

        search.dismiss()

        Multiplayer.isMultiplayer = false
        GlobalManager.getInstance().songService.isGaming = false

        GlobalManager.getInstance().mainScene.show()
    }

    fun show() {

        updateBackground()
        GlobalManager.getInstance().engine.scene = this
        updateList()

        search.show()
    }


    fun init() = Unit
}