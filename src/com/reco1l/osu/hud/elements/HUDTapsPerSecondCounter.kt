package com.reco1l.osu.hud.elements

import com.reco1l.osu.updateThread
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDTapsPerSecondCounter : HUDStatisticCounter("Taps/sec") {

    override val name = "Taps per second counter"

    private val timestamps = ArrayDeque<Float>(50)

    override fun onGameplayTouchDown(time: Float) {
        // Queue to the update thread to avoid the timestamp queue potentially resizing itself in the main thread.
        updateThread { timestamps.add(time) }
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        val earliestElapsedTime = gameScene.elapsedTime - GameHelper.getSpeedMultiplier()

        while (timestamps.isNotEmpty() && timestamps.first() < earliestElapsedTime) {
            timestamps.removeFirst()
        }

        valueText.text = timestamps.size.toString()
    }
}