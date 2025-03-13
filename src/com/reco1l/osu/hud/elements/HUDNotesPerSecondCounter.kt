package com.reco1l.osu.hud.elements

import com.rian.osu.beatmap.hitobject.HitObject
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDNotesPerSecondCounter : HUDStatisticCounter("Notes/sec") {

    override val name = "Notes per second counter"

    private val objects = ArrayDeque<HitObject>()

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        objects.add(obj)
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        val elapsedTimeMs = gameScene.elapsedTime * 1000

        while (objects.isNotEmpty()) {
            val obj = objects.first()

            if (obj.startTime - obj.timePreempt + 1000 * GameHelper.getSpeedMultiplier() >= elapsedTimeMs) {
                break
            }

            objects.removeFirst()
        }

        valueText.text = objects.size.toString()
    }
}