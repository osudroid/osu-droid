package com.reco1l.legacy.ui

import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.MainScene.MusicOption
import ru.nsu.ccfit.zuev.osu.async.AsyncTask
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as global
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as resources

class MainMenu(val main: MainScene)
{

    private val sound = resources().loadSound("menuhit", "sfx/menuhit.ogg", false)

    private val play = object : Sprite(0f, 0f, resources().getTexture("play"))
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
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

            return super.onAreaTouched(touchEvent, localX, localY)
        }
    }

    private val settings = object : Sprite(0f, 0f, resources().getTexture("options"))
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
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

            return super.onAreaTouched(touchEvent, localX, localY)
        }
    }

    private val exit = object : Sprite(0f, 0f, resources().getTexture("exit"))
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
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

            return super.onAreaTouched(touchEvent, localX, localY)
        }
    }

    private val solo = object : Sprite(0f, 0f, resources().getTexture("solo"))
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
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

                object : AsyncTask()
                {
                    override fun run()
                    {
                        global().engine.scene = LoadingScreen().scene
                        global().mainActivity.checkNewSkins()
                        global().mainActivity.checkNewBeatmaps()
                        if (!LibraryManager.INSTANCE.loadLibraryCache(true))
                        {
                            LibraryManager.INSTANCE.scanLibrary()
                            System.gc()
                        }
                        global().songMenu.reload()
                    }

                    override fun onComplete()
                    {
                        main.musicControl(MusicOption.PLAY)
                        global().engine.scene = global().songMenu.getScene()
                        global().songMenu.select()
                    }
                }.execute()
                return true
            }
            return super.onAreaTouched(touchEvent, localX, localY)
        }
    }

    private val multi = object : Sprite(0f, 0f, resources().getTexture("multi"))
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
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

                object : AsyncTask()
                {
                    override fun run()
                    {
                        global().engine.scene = LoadingScreen().scene
                        global().mainActivity.checkNewSkins()
                        global().mainActivity.checkNewBeatmaps()
                        if (!LibraryManager.INSTANCE.loadLibraryCache(true))
                        {
                            LibraryManager.INSTANCE.scanLibrary()
                            System.gc()
                        }
                    }

                    override fun onComplete()
                    {
                        main.musicControl(MusicOption.PLAY)
                    }
                }.execute()
                return true
            }
            return super.onAreaTouched(touchEvent, localX, localY)
        }
    }

    private val back = object : Sprite(0f, 0f, resources().getTexture("back"))
    {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean
        {
            if (touchEvent.isActionDown)
            {
                setColor(0.7f, 0.7f, 0.7f)
                return true
            }

            if (touchEvent.isActionUp)
            {
                setColor(1f, 1f, 1f)
                showFirstMenu(true)
                return true
            }

            return super.onAreaTouched(touchEvent, localX, localY)
        }
    }

    /**
     * Indicates if the player has tapped on `Play`.
     */
    var isSecondMenu = false

    /**
     * This button will switch between `Play` and `Solo`.
     */
    var first = play

    /**
     * This button will switch between `Settings` and `Multiplayer`
     */
    var second = settings

    /**
     * This button will switch between `Exit` and `Back`
     */
    var third = exit

    fun attachButtons()
    {
        main.scene.attachChild(first, 1)
        main.scene.attachChild(second, 1)
        main.scene.attachChild(third, 1)
    }

    private fun detachButtons()
    {
        first.detachSelf()
        second.detachSelf()
        third.detachSelf()

        main.scene.unregisterTouchArea(first)
        main.scene.unregisterTouchArea(second)
        main.scene.unregisterTouchArea(third)
    }

    private fun showSecondMenu()
    {
        if (isSecondMenu) return
        isSecondMenu = true

        global().engine.runOnUpdateThread {

            detachButtons()

            solo.setPosition(first.x, first.y)
            multi.setPosition(second.x, second.y)
            back.setPosition(third.x, third.y)

            solo.setScale(first.scaleX, first.scaleY)
            multi.setScale(second.scaleX, second.scaleY)
            back.setScale(third.scaleX, third.scaleY)

            solo.alpha = first.alpha
            multi.alpha = second.alpha
            back.alpha = third.alpha

            first = solo
            second = multi
            third = back

            attachButtons()

            main.scene.registerTouchArea(first)
            main.scene.registerTouchArea(second)
            main.scene.registerTouchArea(third)
        }
    }

    fun showFirstMenu(withTouch: Boolean)
    {
        if (!isSecondMenu) return
        isSecondMenu = false

        global().engine.runOnUpdateThread {

            detachButtons()

            play.alpha = first.alpha
            settings.alpha = second.alpha
            exit.alpha = third.alpha

            first = play
            second = settings
            third = exit

            attachButtons()

            if (withTouch)
            {
                main.scene.registerTouchArea(first)
                main.scene.registerTouchArea(second)
                main.scene.registerTouchArea(third)
            }
        }
    }
}