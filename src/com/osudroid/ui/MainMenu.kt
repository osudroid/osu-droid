package com.osudroid.ui

import com.osudroid.BuildSettings
import com.osudroid.resources.R
import com.osudroid.ui.v1.SettingsFragment
import com.reco1l.andengine.sprite.UISprite
import com.osudroid.utils.async
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.utils.mainThread
import com.osudroid.multiplayer.LobbyScene
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.RoomScene
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

class MainMenu(val main: MainScene) {


    private val playSound = ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false)

    private val playTexture = ResourceManager.getInstance().getTexture("play")

    private val soloTexture = ResourceManager.getInstance().getTexture("solo")

    private val optionsTexture = ResourceManager.getInstance().getTexture("options")

    private val multiTexture = ResourceManager.getInstance().getTexture("multi")

    private val exitTexture = ResourceManager.getInstance().getTexture("exit")

    private val backTexture = ResourceManager.getInstance().getTexture("back")


    /**
     * This button will switch between `Play` and `Solo`.
     */
    val first = object : UISprite() {

        init {
            textureRegion = playTexture
        }

        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean {

            if (touchEvent.isActionDown) {
                setColor(0.7f, 0.7f, 0.7f)
                playSound?.play()
                return true
            }

            if (touchEvent.isActionUp) {
                setColor(1f, 1f, 1f)

                if (textureRegion == playTexture) {
                    showSecondMenu()
                    return true
                }

                if (main.isOnExitAnim) {
                    return true
                }

                GlobalManager.getInstance().songService.isGaming = true

                async {
                    LoadingScreen().show()

                    GlobalManager.getInstance().mainActivity.checkNewSkins()
                    GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                    if (LibraryManager.getLibrary().isEmpty()) {
                        GlobalManager.getInstance().songService.isGaming = false
                        GlobalManager.getInstance().engine.scene = main.scene

                        BeatmapListing().show()
                    } else {
                        main.musicControl(MainScene.MusicOption.PLAY)

                        GlobalManager.getInstance().songMenu.reload()
                        GlobalManager.getInstance().songMenu.show()
                        GlobalManager.getInstance().songMenu.select()
                    }
                }

                return true
            }
            return false
        }
    }

    /**
     * This button will switch between `Settings` and `Multiplayer`
     */
    val second = object : UISprite() {

        init {
            textureRegion = optionsTexture
        }

        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean {

            if (touchEvent.isActionDown) {
                setColor(0.7f, 0.7f, 0.7f)
                return true
            }

            if (touchEvent.isActionUp) {
                setColor(1f, 1f, 1f)

                if (main.isOnExitAnim) {
                    return true
                }

                if (textureRegion == optionsTexture) {
                    GlobalManager.getInstance().songService.isGaming = true
                    mainThread { SettingsFragment().show() }
                    return true
                }

                if (!OnlineManager.getInstance().isStayOnline && !BuildSettings.MOCK_MULTIPLAYER) {
                    ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                    return true
                }

                GlobalManager.getInstance().songService.isGaming = true
                Multiplayer.isMultiplayer = true

                async {
                    LoadingScreen().show()

                    GlobalManager.getInstance().mainActivity.checkNewSkins()
                    GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                    GlobalManager.getInstance().songMenu.reload()

                    RoomScene.load()
                    LobbyScene.load()
                    LobbyScene.show()
                }
                return true
            }
            return false
        }
    }

    /**
     * This button will switch between `Exit` and `Back`
     */
    val third = object : UISprite() {

        init {
            textureRegion = exitTexture
        }

        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean {

            if (touchEvent.isActionDown) {
                setColor(0.7f, 0.7f, 0.7f)
                return true
            }

            if (touchEvent.isActionUp) {
                setColor(1f, 1f, 1f)

                if (textureRegion == exitTexture) {
                    main.showExitDialog()
                } else {
                    showFirstMenu()
                }

                return true
            }
            return false
        }
    }

    /**
     * List of all buttons in the main menu.
     */
    val buttons = listOf(first, second, third)

    private var isSecondMenu = false


    fun attachButtons() {
        if (!first.hasParent()) {
            main.scene.attachChild(first, 1)
        }

        if (!second.hasParent()) {
            main.scene.attachChild(second, 1)
        }

        if (!third.hasParent()) {
            main.scene.attachChild(third, 1)
        }
    }

    fun detachButtons() {
        main.scene.detachChild(first)
        main.scene.detachChild(second)
        main.scene.detachChild(third)
    }

    fun showFirstMenu() {

        if (!isSecondMenu) {
            return
        }
        isSecondMenu = false

        first.textureRegion = playTexture
        second.textureRegion = optionsTexture
        third.textureRegion = exitTexture
    }

    private fun showSecondMenu() {

        if (isSecondMenu) {
            return
        }
        isSecondMenu = true

        first.textureRegion = soloTexture
        second.textureRegion = multiTexture
        third.textureRegion = backTexture
    }
}