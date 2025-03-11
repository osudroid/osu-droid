package com.reco1l.osu.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import com.reco1l.osu.hud.HUDElement
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


class HUDLinearSongProgress : HUDElement() {

    private val backgroundRect = Box().apply {

        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        relativeSizeAxes = Axes.X
        setSize(1f, BAR_HEIGHT)

        color = ColorARGB.Black
        alpha = 0.3f
    }

    private val progressRect = Box().apply {

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


    fun setProgress(progress: Float, isIntro: Boolean) {

        if (isIntro) {
            progressRect.setColor(153f / 255f, 204f / 255f, 51f / 255f)
        } else {
            progressRect.setColor(1f, 1f, 150f / 255f)
        }

        progressRect.width = drawWidth * progress
    }


    override fun onGameplayUpdate(gameScene: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        if (gameScene.elapsedTime < gameScene.firstObjectStartTime) {
            setProgress((gameScene.elapsedTime - gameScene.initialElapsedTime) / (gameScene.firstObjectStartTime - gameScene.initialElapsedTime), true)
        } else {
            setProgress((gameScene.elapsedTime - gameScene.firstObjectStartTime) / (gameScene.lastObjectEndTime - gameScene.firstObjectStartTime), false)
        }
    }


    companion object {
        private const val BAR_HEIGHT = 7f
    }

}