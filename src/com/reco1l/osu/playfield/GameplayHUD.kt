package com.reco1l.osu.playfield

import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.SCORE
import org.anddev.andengine.engine.camera.hud.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*

class GameplayHUD(private val stat: StatisticV2, private val game: GameScene) : HUD() {


    val healthBar = HealthBar(stat)

    val scoreCounter = ScoreCounter()

    val comboCounter = ComboCounter()

    val accuracyCounter = AccuracyCounter()

    val songProgress = SongProgress()


    init {
        attachChild(healthBar)
        attachChild(scoreCounter)
        attachChild(accuracyCounter)
        attachChild(comboCounter)
        attachChild(songProgress)

        scoreCounter.metric = Config.getScoreCounterMetric()
        scoreCounter.setValue(0)
        scoreCounter.onUpdateText()

        accuracyCounter.y += scoreCounter.y + scoreCounter.height
        accuracyCounter.onUpdateText()
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        comboCounter.setCombo(stat.combo)
        accuracyCounter.setAccuracy(stat.accuracy)

        songProgress.x = accuracyCounter.x - accuracyCounter.widthScaled - 18f
        songProgress.y = accuracyCounter.y + accuracyCounter.heightScaled / 2f

        // PP is updated in `GameScene` class.
        if (Config.getScoreCounterMetric() == SCORE) {
            scoreCounter.setValue(stat.totalScoreWithMultiplier)
        }

        if (game.elapsedTime < game.firstObjectStartTime) {
            songProgress.setProgress(game.elapsedTime / game.firstObjectStartTime, true)
        } else {
            songProgress.setProgress((game.elapsedTime - game.firstObjectStartTime) / (game.lastObjectEndTime - game.firstObjectStartTime), false)
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


