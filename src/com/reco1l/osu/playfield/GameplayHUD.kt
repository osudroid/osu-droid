package com.reco1l.osu.playfield

import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.hud.*
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.*

class GameplayHUD(private val stat: StatisticV2) : HUD() {


    val healthDisplay = HealthDisplay(stat).also {
        // Health display bar is already setup in its constructor.
        attachChild(it)
    }

    val scoreText = ScoreText(OsuSkin.get().scorePrefix).also {

        it.setAnchor(Anchor.TopRight)
        it.setOrigin(Anchor.TopRight)
        it.setScale(0.96f)
        it.text = "0000000000"
        it.x = -10f

        attachChild(it)
    }

    val accuracyText = ScoreText(OsuSkin.get().scorePrefix).also {

        it.setAnchor(Anchor.TopRight)
        it.setOrigin(Anchor.TopRight)
        it.setScale(0.6f * 0.96f)
        it.setPosition(-17f, scoreText.characters['0']!!.height + 9f)
        it.text = "000.00%"

        attachChild(it)
    }

    val comboCounter = ComboCounter().also {
        // Combo counter is already setup in its constructor.
        attachChild(it)
    }


    private val strBuilder = StringBuilder(16)


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        // Combo
        comboCounter.setCombo(stat.combo)

        // Accuracy
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

        // Score
        strBuilder.setLength(0)
        strBuilder.append(stat.getTotalScoreWithMultiplier())
        while (strBuilder.length < 8) {
            strBuilder.insert(0, '0')
        }
        scoreText.text = strBuilder.toString()

        super.onManagedUpdate(pSecondsElapsed)
    }

}