package com.reco1l.osu.playfield

import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.SCORE
import org.anddev.andengine.engine.camera.hud.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.scoring.*

class GameplayHUD(private val stat: StatisticV2) : HUD() {


    val healthBar = HealthBar(stat)

    val scoreCounter = ScoreCounter()

    val comboCounter = ComboCounter()

    val accuracyText = AccuracyCounter()


    init {
        attachChild(healthBar)
        attachChild(scoreCounter)
        attachChild(accuracyText)
        attachChild(comboCounter)

        scoreCounter.metric = Config.getScoreCounterMetric()
        scoreCounter.setValue(0)
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        comboCounter.setCombo(stat.combo)
        accuracyText.setAccuracy(stat.accuracy * 100f)

        // PP is updated in `GameScene` class.
        if (Config.getScoreCounterMetric() == SCORE) {
            scoreCounter.setValue(stat.totalScoreWithMultiplier)
        }

        super.onManagedUpdate(pSecondsElapsed)
    }

}


