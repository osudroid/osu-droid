package com.reco1l.legacy.ui

import com.reco1l.framework.lang.async
import com.reco1l.legacy.ui.multiplayer.LobbyScene
import com.reco1l.legacy.ui.multiplayer.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu
import java.lang.Exception
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as global
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as resources

class MainMenu(val main: MainScene)
{

    private val sound = resources().loadSound("menuhit", "sfx/menuhit.ogg", false)

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
            }
            else
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

                    global().songService.isGaming = true

                    async {
                        LoadingScreen().show()

                        global().mainActivity.checkNewSkins()
                        global().mainActivity.checkNewBeatmaps()
                        LibraryManager.INSTANCE.updateLibrary(true)

                        main.musicControl(MusicOption.PLAY)

                        global().songMenu.reload()
                        global().songMenu.show()
                        global().songMenu.select()
                    }
                    return true
                }
            }
            return super.onAreaTouched(touchEvent, localX, localY)
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
                    global().songService.isGaming = true
                    global().mainActivity.runOnUiThread { SettingsMenu().show() }
                    return true
                }
            }
            else
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

                    global().songService.isGaming = true
                    Multiplayer.isMultiplayer = true

                    async {
                        LoadingScreen().show()

                        global().mainActivity.checkNewSkins()
                        global().mainActivity.checkNewBeatmaps()
                        LibraryManager.INSTANCE.updateLibrary(true)

                        LobbyScene.load()
                        RoomScene.load()

                        try
                        {
                            LobbyScene.show()
                        }
                        catch (e: Exception)
                        {
                            global().songService.isGaming = false
                            Multiplayer.isMultiplayer = false

                            global().mainScene.show()
                            ToastLogger.showText("Unable to connect multiplayer services: ${e.message}", true)
                            e.printStackTrace()
                            return@async
                        }
                    }
                    return true
                }
            }
            return super.onAreaTouched(touchEvent, localX, localY)
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
            }
            else
            {
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
            }
            return super.onAreaTouched(touchEvent, localX, localY)
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