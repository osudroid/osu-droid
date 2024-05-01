package com.reco1l.osu.ui

import com.reco1l.osu.async
import com.reco1l.osu.multiplayer.LobbyScene
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.beatmaplisting.BeatmapListing
import com.reco1l.osu.multiplayer.RoomScene
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

class MainMenu(val main: MainScene)
{

    private val sound = getResources().loadSound("menuhit", "sfx/menuhit.ogg", false)

    /**
     * This button will switch between `Play` and `Solo`.
     */
    val first = object : AnimSprite(0f, 0f, 0f, "play", "solo")
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {
            if (frame == 0)
            {
                if (touchEvent.isActionDown)
                {
                    setColor(0.7f, 0.7f, 0.7f)
                    sound?.play()
                    return true
                }

                if (touchEvent.isActionUp)
                {
                    setColor(1f, 1f, 1f)
                    showSecondMenu()
                    return true
                }
                return false
            }

            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                sound?.play()
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)

                if (main.isOnExitAnim)
                    return true

                getGlobal().songService.isGaming = true

                async {
                    LoadingScreen().show()

                    getGlobal().mainActivity.checkNewSkins()
                    getGlobal().mainActivity.checkNewBeatmaps()
                    LibraryManager.INSTANCE.updateLibrary(true)

                    if (LibraryManager.INSTANCE.library.isEmpty())
                    {
                        getGlobal().songService.isGaming = false
                        getGlobal().engine.scene = main.scene

                        BeatmapListing.show()
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
    val second = object : AnimSprite(0f, 0f, 0f, "options", "multi")
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {

            if (frame == 0)
            {
                if (touchEvent.isActionDown)
                {
                    setColor(0.7f, 0.7f, 0.7f)
                    sound?.play()
                    return true
                }

                if (touchEvent.isActionUp)
                {
                    setColor(1f, 1f, 1f)
                    if (main.isOnExitAnim) return true
                    getGlobal().songService.isGaming = true
                    getGlobal().mainActivity.runOnUiThread { SettingsFragment().show() }
                    return true
                }
                return false
            }

            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                sound?.play()
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)

                if (!getOnline().isStayOnline) {
                    ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                    return true
                }

                if (main.isOnExitAnim) return true

                getGlobal().songService.isGaming = true
                Multiplayer.isMultiplayer = true

                async {
                    LoadingScreen().show()

                    getGlobal().mainActivity.checkNewSkins()
                    getGlobal().mainActivity.checkNewBeatmaps()
                    LibraryManager.INSTANCE.updateLibrary(true)

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
    val third = object : AnimSprite(0f, 0f, 0f, "exit", "back")
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {
            if (frame == 0)
            {
                if (touchEvent.isActionDown)
                {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }

                if (touchEvent.isActionUp)
                {
                    setColor(1f, 1f, 1f)
                    main.showExitDialog()
                    return true
                }
                return false
            }

            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)
                showFirstMenu()
                return true
            }
            return false
        }
    }

    /**
     * Indicates if the player has tapped on `Play`.
     */
    private var isSecondMenu = false


    fun attachButtons()
    {
        main.scene.attachChild(first, 1)
        main.scene.attachChild(second, 1)
        main.scene.attachChild(third, 1)
    }

    private fun showSecondMenu()
    {
        if (isSecondMenu) return
        isSecondMenu = true

        first.frame = 1
        second.frame = 1
        third.frame = 1
    }

    fun showFirstMenu()
    {
        if (!isSecondMenu) return
        isSecondMenu = false

        first.frame = 0
        second.frame = 0
        third.frame = 0
    }
}