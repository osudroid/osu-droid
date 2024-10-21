package com.reco1l.osu.playfield

import androidx.annotation.*
import com.reco1l.osu.playfield.ProgressIndicatorType.Companion.TOP_RIGHT_PIE
import com.reco1l.osu.playfield.ProgressIndicatorType.Companion.BOTTOM_LONG
import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.SCORE
import org.anddev.andengine.engine.camera.hud.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*

class GameplayHUD(private val stat: StatisticV2, private val game: GameScene, private val withStatistics: Boolean) : HUD() {


    private val healthBar: HealthBar?

    private val scoreCounter: ScoreCounter?

    private val songProgress: CircularSongProgress?

    private val comboCounter: ComboCounter?

    private val accuracyCounter: AccuracyCounter?


    init {
        if (withStatistics) {
            healthBar = HealthBar(stat)
            attachChild(healthBar)

            scoreCounter = ScoreCounter()
            attachChild(scoreCounter)

            comboCounter = ComboCounter()
            attachChild(comboCounter)

            accuracyCounter = AccuracyCounter()
            attachChild(accuracyCounter)

            songProgress = CircularSongProgress()
            attachChild(songProgress)

            scoreCounter.metric = Config.getScoreCounterMetric()
            scoreCounter.setValue(0)
            scoreCounter.onUpdateText()

            accuracyCounter.y += scoreCounter.y + scoreCounter.height
            accuracyCounter.onUpdateText()

        } else {
            healthBar = null
            scoreCounter = null
            comboCounter = null
            accuracyCounter = null
            songProgress = null
        }

    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (withStatistics) {
            comboCounter!!.setCombo(stat.combo)
            accuracyCounter!!.setAccuracy(stat.accuracy)

            // PP is updated in `GameScene` class.
            if (Config.getScoreCounterMetric() == SCORE) {
                scoreCounter!!.setValue(stat.totalScoreWithMultiplier)
            }

            if (Config.getProgressIndicatorType() == TOP_RIGHT_PIE) {
                songProgress!!.x = accuracyCounter.x - accuracyCounter.widthScaled - 18f
                songProgress.y = accuracyCounter.y + accuracyCounter.heightScaled / 2f

                if (game.elapsedTime < game.firstObjectStartTime) {
                    songProgress.setProgress((game.elapsedTime - game.initialElapsedTime) / (game.firstObjectStartTime - game.initialElapsedTime), true)
                } else {
                    songProgress.setProgress((game.elapsedTime - game.firstObjectStartTime) / (game.lastObjectEndTime - game.firstObjectStartTime), false)
                }
            }
        }


        super.onManagedUpdate(pSecondsElapsed)
    }


    fun setHealthBarVisibility(visible: Boolean) {
        healthBar?.isVisible = visible
    }

    fun flashHealthBar() {
        healthBar?.flash()
    }

    fun setScoreCounterText(score: String) {
        scoreCounter?.text = score
    }

}


@IntDef(TOP_RIGHT_PIE, BOTTOM_LONG)
annotation class ProgressIndicatorType {
    companion object {
        const val TOP_RIGHT_PIE = 0
        const val BOTTOM_LONG = 1
    }
}


