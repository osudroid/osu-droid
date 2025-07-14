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
        onContentChanged()
    }

    fun setValue(value: Double) {
        // The NaN check here is necessary as calculations may result in NaN values, which should not be displayed.
        // This is not a foolproof solution, but is enough to prevent crashes from happening.
        // Until the source of the NaN values is found and fixed, this must remain in place.
        sprite.text = "${(value.takeIf { !it.isNaN() } ?: 0.0).roundToInt()}${if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid) "dpp" else "pp"}"
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        setValue(game.stat.pp)
    }
}