package com.reco1l.osu.playfield

import androidx.annotation.*
import com.reco1l.andengine.*
import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.PP
import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.SCORE
import org.anddev.andengine.engine.camera.hud.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.*

class GameplayHUD(private val stat: StatisticV2, showStatistics: Boolean) : HUD() {


    private val healthDisplay: HealthDisplay?

    private val comboCounter: ComboCounter?

    private val accuracyText: ScoreText?

    private val scoreText: ScoreText?

    private val strBuilder = StringBuilder(16)


    init {


        if (showStatistics) {
            healthDisplay = HealthDisplay(stat)
            attachChild(healthDisplay)

            scoreText = ScoreText(OsuSkin.get().scorePrefix)
            scoreText.setAnchor(Anchor.TopRight)
            scoreText.setOrigin(Anchor.TopRight)
            scoreText.setScale(0.96f)
            scoreText.text = when(Config.getScoreCounterMetric()) {
                SCORE -> "00000000"
                PP -> "0.00"
                else -> ""
            }
            scoreText.x = -10f
            attachChild(scoreText)

            accuracyText = ScoreText(OsuSkin.get().scorePrefix)
            accuracyText.setAnchor(Anchor.TopRight)
            accuracyText.setOrigin(Anchor.TopRight)
            accuracyText.setScale(0.6f * 0.96f)
            accuracyText.setPosition(-17f, scoreText.characters['0']!!.height + 9f)
            accuracyText.text = "000.00%"
            attachChild(accuracyText)

            comboCounter = ComboCounter()
            attachChild(comboCounter)

        } else {
            healthDisplay = null
            accuracyText = null
            comboCounter = null
            scoreText = null
        }

    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        // Combo
        comboCounter?.setCombo(stat.combo)

        // Accuracy
        if (accuracyText != null) {
            strBuilder.setLength(0)
            var rawAccuracy = stat.accuracy * 100f
            strBuilder.append(rawAccuracy.toInt())
            if (rawAccuracy.toInt() < 10) {
                strBuilder.insert(0, '0')
            }
            strBuilder.append('.')
            rawAccuracy -= rawAccuracy.toInt().toFloat()
            rawAccuracy *= 100f
            if (rawAccuracy.toInt() < 10) {
                strBuilder.append('0')
            }
            strBuilder.append(rawAccuracy.toInt())
            strBuilder.append('%')
            accuracyText.text = strBuilder.toString()
        }


        // Score
        if (scoreText != null && Config.getScoreCounterMetric() == SCORE) {
            strBuilder.setLength(0)
            strBuilder.append(stat.getTotalScoreWithMultiplier())
            while (strBuilder.length < 8) {
                strBuilder.insert(0, '0')
            }
            scoreText.text = strBuilder.toString()
        }

        super.onManagedUpdate(pSecondsElapsed)
    }


    fun setHealthBarVisibility(visible: Boolean) {
        healthDisplay?.isVisible = visible
    }

    fun flashHealthBar() {
        healthDisplay?.flash()
    }

    fun setScoreCounterText(score: String) {
        scoreText?.text = score
    }

}


/**
 * Defines the metric to be used by the score counter.
 */
@IntDef(SCORE, PP)
annotation class ScoreCounterMetric {
    companion object {
        const val SCORE = 0
        const val PP = 1
    }
}