package com.reco1l.osu.ui.entity

import com.osudroid.resources.R.*
import com.reco1l.andengine.sprite.*
import com.reco1l.osu.async
import com.reco1l.osu.multiplayer.LobbyScene
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.beatmaplisting.BeatmapListing
import com.reco1l.osu.mainThread
import com.reco1l.osu.multiplayer.RoomScene
import com.reco1l.osu.ui.SettingsFragment
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

class MainMenu(val main: MainScene) {


    private val playSound = getResources().loadSound("menuhit", "sfx/menuhit.ogg", false)

    private val playTexture = getResources().getTexture("play")

    private val soloTexture = getResources().getTexture("solo")

    private val optionsTexture = getResources().getTexture("options")

    private val multiTexture = getResources().getTexture("multi")

    private val exitTexture = getResources().getTexture("exit")

    private val backTexture = getResources().getTexture("back")


    /**
     * This button will switch between `Play` and `Solo`.
     */
    val first = object : ExtendedSprite() {

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

                getGlobal().songService.isGaming = true

                async {
                    LoadingScreen().show()

                    getGlobal().mainActivity.checkNewSkins()
                    getGlobal().mainActivity.loadBeatmapLibrary()

                    if (LibraryManager.getLibrary().isEmpty()) {
                        getGlobal().songService.isGaming = false
                        getGlobal().engine.scene = main.scene

                        BeatmapListing().show()
                    } else {
                        main.musicControl(MusicOption.PLAY)

                        getGlobal().songMenu.reload()
                        getGlobal().songMenu.show()
                        getGlobal().songMenu.select()
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
    val second = object : ExtendedSprite() {

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
                    getGlobal().songService.isGaming = true
                    mainThread { SettingsFragment().show() }
                    return true
                }

                if (!getOnline().isStayOnline) {
                    ToastLogger.showText(StringTable.format(string.multiplayer_not_online), true)
                    return true
                }

                getGlobal().songService.isGaming = true
                Multiplayer.isMultiplayer = true

                async {
                    LoadingScreen().show()

                    getGlobal().mainActivity.checkNewSkins()
                    getGlobal().mainActivity.loadBeatmapLibrary()

                    getGlobal().songMenu.reload()

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
    val third = object : ExtendedSprite() {

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


    private var isSecondMenu = false


    fun attachButtons() {
        main.scene.attachChild(first, 1)
        main.scene.attachChild(second, 1)
        main.scene.attachChild(third, 1)
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