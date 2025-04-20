package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.roundToInt

class HUDPPCounter : HUDElement() {

    override val name = "PP counter"

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)

    init {
        setValue(0.0)
        attachChild(sprite)
        onMeasureContentSize()
    }

    fun setValue(value: Double) {
        sprite.text = "${value.roundToInt()}${if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid) "dpp" else "pp"}"
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        setValue(game.stat.pp)
    }
}