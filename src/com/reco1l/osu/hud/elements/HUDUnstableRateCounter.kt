package com.reco1l.osu.hud.elements

import com.reco1l.andengine.text.ExtendedText
import com.reco1l.osu.hud.HUDElement
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class HUDUnstableRateCounter : HUDElement() {

    private val text = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = "UR: 0.00"
    }

    init {
        attachChild(text)
    }

    override fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        text.text = "UR: %.2f".format(statistics.unstableRate)
    }
}