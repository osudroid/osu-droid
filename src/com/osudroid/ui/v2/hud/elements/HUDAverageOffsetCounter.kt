package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.text.UIText
import com.osudroid.ui.v2.hud.HUDElement
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import kotlin.math.roundToInt

class HUDAverageOffsetCounter : HUDElement() {

    override val name = "Average offset counter"

    private val text = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = "Avg offset: 0ms"
    }

    init {
        attachChild(text)
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        val avgOffset = if (game.offsetRegs > 0) game.offsetSum / game.offsetRegs else 0f
        text.text = "Avg offset: ${(avgOffset * 1000).roundToInt()}ms"
    }
}