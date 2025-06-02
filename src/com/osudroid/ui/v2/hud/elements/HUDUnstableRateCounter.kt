package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.text.UIText
import com.osudroid.ui.v2.hud.HUDElement
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene

class HUDUnstableRateCounter : HUDElement() {

    override val name = "Unstable rate counter"

    private val text = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = "UR: 0.00"
    }

    init {
        attachChild(text)
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        text.text = "UR: %.2f".format(game.stat.unstableRate)
    }
}