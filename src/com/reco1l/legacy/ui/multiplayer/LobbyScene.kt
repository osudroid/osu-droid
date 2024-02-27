package com.reco1l.legacy.ui.multiplayer

import android.net.Uri
import com.dgsrz.bancho.security.SecurityUtils
import com.reco1l.api.ibancho.LobbyAPI
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.orAsyncCatch
import com.reco1l.framework.lang.glThread
import com.reco1l.legacy.Multiplayer
import org.andengine.entity.modifier.LoopEntityModifier
import org.andengine.entity.modifier.RotationByModifier
import org.andengine.entity.primitive.Rectangle
import org.andengine.entity.scene.Scene
import org.andengine.entity.scene.background.SpriteBackground
import org.andengine.entity.sprite.Sprite
import org.andengine.entity.text.Text
import org.andengine.input.touch.TouchEvent
import org.andengine.util.math.MathUtils
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object LobbyScene : Scene()
{

    /**The search fragment*/
    val search by lazy { LobbySearch() }

    /**The search query*/
    var searchQuery: String? = null
        set(value)
        {
            field = value

            if (!awaitList)
                updateList()
        }


    private var backButton: Sprite? = null

    private var createButton: TextButton? = null

    private var refreshButton: TextButton? = null


    private val roomList = LobbyRoomList()

    private val onlinePanel = OnlinePanel()

    private val titleText = Text(20f, 20f, getResources().getFont("bigFont"), "", 100, GlobalManager.getInstance().engine.vertexBufferObjectManager)

    private val infoText = Text(20f, 0f, getResources().getFont("smallFont"), "", 100, GlobalManager.getInstance().engine.vertexBufferObjectManager)

    private val loading = Sprite(0f, 0f, getResources().getTexture("loading_start"), GlobalManager.getInstance().engine.vertexBufferObjectManager)


    /**Await lock for the list refresh*/
    private var awaitList = false


    fun load()
    {
        detachChildren()
        clearTouchAreas()

        isBackgroundEnabled = true
        updateBackground()

        // Background dim
        val dim = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat(), GlobalManager.getInstance().engine.vertexBufferObjectManager)
        dim.setColor(0f, 0f, 0f, 0.5f)
        attachChild(dim)

        // Top bar
        val top = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 120f, GlobalManager.getInstance().engine.vertexBufferObjectManager)
        top.setColor(0f, 0f, 0f, 0.3f)
        attachChild(top)

        // Title
        titleText.text = "Multiplayer Lobby"
        attachChild(titleText)

        // Info
        infoText.setPosition(20f, titleText.y + titleText.height)
        infoText.setColor(0.8f, 0.8f, 0.8f)
        attachChild(infoText)

        // Room list
        attachChild(roomList, 1)

        // Loading
        loading.setPosition((Config.getRES_WIDTH() - loading.width) / 2f, (Config.getRES_HEIGHT() - loading.height) / 2f)
        loading.setRotationCenter(loading.width / 2f, loading.height / 2f)
        loading.setScale(0.4f)
        attachChild(loading)

        // Back button code copy and paste from legacy code but improved, don't blame on me.
        val layoutBackButton = OsuSkin.get().getLayout("BackButton")
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

                    ResourceManager.getInstance().getSound("menuback")?.play()
                    return true
                }
                if (event.isActionUp)
                {
                    this.setScale(1f)

                    if (!moved)
                        back()
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

            attachChild(it)
            registerTouchArea(it)
        }

        // Online panel
        onlinePanel.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
        attachChild(onlinePanel)

        createButton = object : TextButton(getResources().getFont("CaptionFont"), "Create New Room")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitList)
                    return false

                getResources().getSound("menuclick")?.play()
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

        refreshButton = object : TextButton(getResources().getFont("CaptionFont"), "Refresh")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp || awaitList)
                    return false

                getResources().getSound("menuclick")?.play()
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


    // Connection

    fun connectFromLink(link: Uri)
    {
        if (Multiplayer.isConnected)
            return

        getGlobal().songService.isGaming = true
        Multiplayer.isMultiplayer = true

        {
            LoadingScreen().show()

            getGlobal().mainActivity.checkNewSkins()
            getGlobal().mainActivity.checkNewBeatmaps()
            LibraryManager.INSTANCE.updateLibrary(true)

            RoomScene.load()
            load()

            val roomID = link.pathSegments[0].toLong()
            val password = if (link.pathSegments.size > 1) link.pathSegments[1] else null

            RoomAPI.connectToRoom(roomID, getOnline().userId, getOnline().username, password)

        }.orAsyncCatch {

            ToastLogger.showText("Failed to connect to the room: ${it.className} - ${it.message}", true)
            it.printStackTrace()
            back()
        }
    }

    // Update events

    @JvmStatic
    fun updateOnlinePanel() = glThread {

        onlinePanel.setInfo()
        onlinePanel.setAvatar()
    }

    fun updateList()
    {
        // Hiding buttons
        createButton?.isVisible = false
        refreshButton?.isVisible = false

        // Updating info text
        infoText.text = "Loading room list..."

        // Detaching list children
        roomList.detachChildren()

        // Showing loading icon
        loading.isVisible = true
        loading.registerEntityModifier(LoopEntityModifier(RotationByModifier(2f, 360f)))

        awaitList = true
        {
            val list = LobbyAPI.getRooms(searchQuery, SecurityUtils.signRequest(searchQuery ?: ""))

            // Updating list
            glThread {
                roomList.setList(list)
                awaitList = false
                val roomCount = roomList.childCount

                // Updating info text
                infoText.text = if (searchQuery.isNullOrEmpty())
                {
                    "Showing $roomCount ${if (roomCount == 1) "match" else "matches"}."
                }
                else "Searching for \"$searchQuery\", showing $roomCount ${if (roomCount == 1) "result" else "results"}."
            }

            // Hiding loading icon
            loading.isVisible = false
            loading.clearEntityModifiers()

            // Showing buttons
            createButton?.isVisible = true
            refreshButton?.isVisible = true

        }.orAsyncCatch {

            awaitList = false
            it.printStackTrace()

            back()
            ToastLogger.showText("Multiplayer server is unavailable, try again later.", true)
        }
    }

    private fun updateBackground()
    {
        var texture = getResources().getTexture("menu-background")

        if (!Config.isSafeBeatmapBg())
                texture = getResources().getTexture("::background") ?: texture

        texture?.also {

            val height = it.height * (Config.getRES_WIDTH() / it.width.toFloat())
            val width = Config.getRES_WIDTH().toFloat()

            background = SpriteBackground(Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, width, height, it, GlobalManager.getInstance().engine.vertexBufferObjectManager))
        }
    }


    // Navigation

    override fun back()
    {
        if (awaitList)
            return

        search.dismiss()

        Multiplayer.isMultiplayer = false
        getGlobal().songService.isGaming = false

        getGlobal().mainScene.show()
    }

    fun show()
    {
        updateBackground()
        getGlobal().engine.scene = this
        updateList()

        search.show()
    }


    fun init() = Unit
}