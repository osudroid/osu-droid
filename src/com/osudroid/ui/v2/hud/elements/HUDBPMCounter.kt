package com.osudroid.ui.v2.hud.elements

import com.rian.framework.RollingIntCounter
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDBPMCounter : HUDStatisticCounter("BPM") {

    override val name = "BPM counter"

    private var value = 0
        set(value) {
            if (field != value) {
                field = value
                valueText.text = value.toString()
            }
        }

    private val counter = RollingIntCounter(0).apply { rollingDuration = 0.375f }

    init {
        registerUpdateHandler(counter)
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        val beatmap = gameScene.playableBeatmap ?: return

        val timingPoint = beatmap.controlPoints.timing.controlPointAt(gameScene.elapsedTime * 1000.0)
        val bpm = timingPoint.bpm * GameHelper.getSpeedMultiplier()

        counter.targetValue = bpm.roundToInt()
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        value = counter.currentValue

        super.onManagedUpdate(deltaTimeSec)
    }
}