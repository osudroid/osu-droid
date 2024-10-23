package com.reco1l.osu.playfield

import androidx.annotation.*
import com.reco1l.osu.playfield.ProgressIndicatorType.Companion.PIE
import com.reco1l.osu.playfield.ProgressIndicatorType.Companion.BAR
import org.anddev.andengine.engine.camera.hud.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*

class GameplayHUD(private val stat: StatisticV2, private val game: GameScene, private val withStatistics: Boolean) : HUD() {


    private val healthBar: HealthBar?

    private val ppCounter: PPCounter?

    private val scoreCounter: ScoreCounter?

    private val comboCounter: ComboCounter?

    private val pieSongProgress: CircularSongProgress?

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

            if (Config.getProgressIndicatorType() == PIE) {
                pieSongProgress = CircularSongProgress()
                attachChild(pieSongProgress)
            } else {
                pieSongProgress = null
            }

            if (Config.isDisplayRealTimePPCounter()) {
                ppCounter = PPCounter(Config.getDifficultyAlgorithm())
                attachChild(ppCounter)
            } else {
                ppCounter = null
            }

            scoreCounter.setScore(0)
            scoreCounter.onUpdateText()

            accuracyCounter.y += scoreCounter.y + scoreCounter.height
            accuracyCounter.onUpdateText()

        } else {
            healthBar = null
            ppCounter = null
            scoreCounter = null
            comboCounter = null
            accuracyCounter = null
            pieSongProgress = null
        }

    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (withStatistics) {
            comboCounter!!.setCombo(stat.combo)
            scoreCounter!!.setScore(stat.totalScoreWithMultiplier)
            accuracyCounter!!.setAccuracy(stat.accuracy)

            if (Config.getProgressIndicatorType() == PIE) {
                pieSongProgress!!.x = accuracyCounter.x - accuracyCounter.widthScaled - 18f
                pieSongProgress.y = accuracyCounter.y + accuracyCounter.heightScaled / 2f

                if (Config.isDisplayRealTimePPCounter()) {
                    ppCounter!!.x = pieSongProgress.x - pieSongProgress.widthScaled - 18f
                    ppCounter.y = pieSongProgress.drawY
                }

                if (game.elapsedTime < game.firstObjectStartTime) {
                    pieSongProgress.setProgress((game.elapsedTime - game.initialElapsedTime) / (game.firstObjectStartTime - game.initialElapsedTime), true)
                } else {
                    pieSongProgress.setProgress((game.elapsedTime - game.firstObjectStartTime) / (game.lastObjectEndTime - game.firstObjectStartTime), false)
                }
            } else if (Config.isDisplayRealTimePPCounter()) {
                ppCounter!!.x = accuracyCounter.x - accuracyCounter.widthScaled - 18f
                ppCounter.y = accuracyCounter.drawY
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

    fun setPPCounterValue(pp: Double) {
        ppCounter?.setValue(pp)
    }

}


@IntDef(PIE, BAR)
annotation class ProgressIndicatorType {
    companion object {
        const val PIE = 0
        const val BAR = 1
    }
}


