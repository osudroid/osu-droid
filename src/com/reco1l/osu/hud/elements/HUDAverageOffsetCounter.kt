package com.reco1l.osu.hud.elements

import com.reco1l.andengine.text.ExtendedText
import com.reco1l.osu.hud.HUDElement
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.roundToInt

class HUDAverageOffsetCounter : HUDElement() {

    private val text = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = "Avg offset: 0ms"
    }

    init {
        attachChild(text)
    }

    override fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        val avgOffset = if (game.offsetRegs > 0) game.offsetSum / game.offsetRegs else 0f
        text.text = "Avg offset: ${(avgOffset * 1000).roundToInt()}ms"
    }
}