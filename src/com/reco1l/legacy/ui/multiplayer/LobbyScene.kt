package com.reco1l.legacy.ui.multiplayer

import android.net.Uri
import com.reco1l.api.ibancho.LobbyAPI
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.orAsyncCatch
import com.reco1l.framework.lang.glThread
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
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.TextButton
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlinePanel
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as global
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as resources
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

object LobbyScene : Scene()
{

    /**The search fragment*/
    val search = LobbySearch()

    /**The search query*/
    var searchQuery: String? = null
        set(value)
        {
            field = value
            updateList()
        }


    private var backButton: Sprite? = null

    private var onlinePanel: OnlinePanel? = null

    private var createButton: TextButton? = null

    private var refreshButton: TextButton? = null


    private val roomList = LobbyRoomList()

    private val titleText = ChangeableText(20f, 20f, resources().getFont("bigFont"), "", 100)

    private val infoText = ChangeableText(20f, 0f, resources().getFont("smallFont"), "", 100)

    private val loading = Sprite(0f, 0f, resources().getTexture("loading_start"))


    /**Await lock for the list refresh*/
    private var awaitList = false


    fun load()
    {
        detachChildren()
        clearTouchAreas()

        isBackgroundEnabled = true
        updateBackground()

        // Background dim
        val dim = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        dim.setColor(0f, 0f, 0f, 0.5f)
        attachChild(dim)

        // Top bar
        val top = Rectangle(0f, 0f, Config.getRES_WIDTH().toFloat(), 120f)
        top.setColor(0f, 0f, 0f, 0.3f)
        attachChild(top)

        // Title
        titleText.text = "Multiplayer lobby"
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

        if (resources().isTextureLoaded("menu-back-0"))
        {
            for (i in 0..59)
                if (resources().isTextureLoaded("menu-back-$i")) loadedBackTextures.add("menu-back-$i")
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
        onlinePanel = OnlinePanel().also {

            it.setPosition(Config.getRES_WIDTH() - 410f - 6f, 6f)
            attachChild(it)
        }

        createButton = object : TextButton(resources().getFont("CaptionFont"), "Create new room")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (!event.isActionUp) return false

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

        refreshButton = object : TextButton(resources().getFont("CaptionFont"), "Refresh")
        {
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                if (event.isActionUp)
                {
                    updateList()
                    return true
                }
                return false
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

        GlobalManager.getInstance().songService.isGaming = true
        Multiplayer.isMultiplayer = true

        {
            LoadingScreen().show()

            GlobalManager.getInstance().mainActivity.checkNewSkins()
            GlobalManager.getInstance().mainActivity.checkNewBeatmaps()
            LibraryManager.INSTANCE.updateLibrary(true)

            RoomScene.load()
            load()

            val roomID = link.pathSegments[0].toLong()
            val password = if (link.pathSegments.size > 1) link.pathSegments[1] else null

            RoomAPI.connectToRoom(roomID, getOnline().userId, getOnline().username, password)

        }.orAsyncCatch {

            ToastLogger.showText("Failed to connect room: ${it.className} - ${it.message}", true)
            it.printStackTrace()
            back()
        }
    }

    // Update events

    fun updateList()
    {
        // Hiding buttons
        search.dismiss()
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
            val list = LobbyAPI.getRooms(searchQuery)

            // Updating list
            glThread {
                roomList.setList(list)
                awaitList = false
            }

            // Hiding loading icon
            loading.isVisible = false
            loading.clearEntityModifiers()

            // Showing buttons
            createButton?.isVisible = true
            refreshButton?.isVisible = true

            // Updating info text
            infoText.text = if (searchQuery.isNullOrEmpty())
            {
                "Showing ${roomList.childCount} matches."
            }
            else "Searching for \"$searchQuery\", showing ${roomList.childCount} results."

            // Showing search fragment
            search.show()

        }.orAsyncCatch {

            awaitList = false
            it.printStackTrace()

            back()
            ToastLogger.showText("Multiplayer server is unavailable, try again later.", true)
        }
    }

    private fun updateBackground()
    {
        (resources().getTexture("::background") ?: resources().getTexture("menu-background")).also {

            val height = it.height.toFloat() * (Config.getRES_WIDTH() / it.width.toFloat())
            val menuBg = Sprite(0f, (Config.getRES_HEIGHT() - height) / 2f, Config.getRES_WIDTH().toFloat(), height, it)

            background = SpriteBackground(menuBg)
        }
    }


    // Navigation

    override fun back()
    {
        if (awaitList)
            return

        search.dismiss()

        Multiplayer.isMultiplayer = false
        global().songService.isGaming = false

        global().mainScene.show()
    }

    fun show()
    {
        updateBackground()
        global().engine.scene = this
        updateList()
    }


    fun init()
    {
    }
}