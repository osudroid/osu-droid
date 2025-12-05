package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.shape.UICircle
import com.reco1l.framework.Color4
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.Size
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene

sealed class HUDSongProgress : HUDElement() {
    abstract fun setProgress(progress: Float, isIntro: Boolean)

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        if (gameScene.elapsedTime < gameScene.firstObjectStartTime) {
            setProgress((gameScene.elapsedTime - gameScene.initialElapsedTime) / (gameScene.firstObjectStartTime - gameScene.initialElapsedTime), true)
        } else {
            setProgress((gameScene.elapsedTime - gameScene.firstObjectStartTime) / (gameScene.lastObjectEndTime - gameScene.firstObjectStartTime), false)
        }
    }
}

class HUDLinearSongProgress : HUDSongProgress() {

    private val backgroundRect = UIBox().apply {
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        width = Size.Full
        height = BAR_HEIGHT
        color = Color4.Black
        alpha = 0.3f
    }

    private val progressRect = UIBox().apply {
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        setSize(0f, BAR_HEIGHT)
        alpha = 0.4f
    }


    init {
        // Adding a 20epx padding so the user can grab the progress bar more easily.
        setSize(Config.getRES_WIDTH().toFloat(), BAR_HEIGHT + 20f)

        attachChild(backgroundRect)
        attachChild(progressRect)
    }


    override fun setProgress(progress: Float, isIntro: Boolean) {

        if (isIntro) {
            progressRect.setColor(153f / 255f, 204f / 255f, 51f / 255f)
        } else {
            progressRect.setColor(1f, 1f, 150f / 255f)
        }

        progressRect.width = width * progress
    }


    companion object {
        private const val BAR_HEIGHT = 7f
    }

}

class HUDPieSongProgress : HUDSongProgress() {

    private val circularProgress: UICircle


    init {
        width = Size.Auto
        height = Size.Auto

        // Reference: https://github.com/ppy/osu/blob/6455c0583b5e607baeca7f584410bc63515aa619/osu.Game/Skinning/LegacySongProgress.cs

        UICircle().also { clear ->

            clear.setSize(30f, 30f)
            clear.anchor = Anchor.Center
            clear.origin = Anchor.Center
            clear.color = Color4.Transparent
            clear.clearInfo = ClearInfo.ClearDepthBuffer
            clear.depthInfo = DepthInfo.Less

            attachChild(clear)
        }

        UICircle().also { background ->

            background.setSize(33f, 33f)
            background.anchor = Anchor.Center
            background.origin = Anchor.Center
            background.color = Color4.White
            background.depthInfo = DepthInfo.Default

            attachChild(background)
        }

        circularProgress = UICircle().also { progress ->

            progress.setSize(30f, 30f)
            progress.anchor = Anchor.Center
            progress.origin = Anchor.Center
            progress.alpha = 0.6f

            attachChild(progress)
        }


        UICircle().also { dot ->

            dot.setSize(4f, 4f)
            dot.anchor = Anchor.Center
            dot.origin = Anchor.Center
            dot.color = Color4.White

            attachChild(dot)
        }
    }


    override fun setProgress(progress: Float, isIntro: Boolean) {

        if (isIntro) {
            circularProgress.setColor(199f / 255f, 1f, 47f / 255f)
            circularProgress.setPortion(-1f + progress)
        } else {
            circularProgress.setColor(1f, 1f, 1f)
            circularProgress.setPortion(progress)
        }

    }

}