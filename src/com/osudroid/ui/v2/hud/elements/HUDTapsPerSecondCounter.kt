package com.osudroid.ui.v2.hud.elements

import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDTapsPerSecondCounter : HUDStatisticCounter("Taps/sec") {

    override val name = "Taps per second counter"

    private val timestamps = ArrayDeque<Float>(50)

    override fun onGameplayTouchDown(time: Float) {
        timestamps.add(time)
    }

    override fun onSeek() {
        timestamps.clear()
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        val earliestElapsedTime = gameScene.elapsedTime - GameHelper.getSpeedMultiplier()

        while (timestamps.isNotEmpty() && timestamps.first() < earliestElapsedTime) {
            timestamps.removeFirst()
        }

        valueText.text = timestamps.size.toString()
    }
}