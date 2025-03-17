package com.reco1l.osu.hud.elements

import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDBPMCounter : HUDStatisticCounter("BPM") {

    override val name = "BPM counter"

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        val beatmap = gameScene.playableBeatmap

        if (beatmap == null) {
            return
        }

        val timingPoint = beatmap.controlPoints.timing.controlPointAt(gameScene.elapsedTime * 1000.0)
        val bpm = timingPoint.bpm * GameHelper.getSpeedMultiplier()

        valueText.text = "%d".format(bpm.roundToInt())
    }
}