package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import com.rian.framework.RollingDoubleCounter
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.math.roundToInt

class HUDPPCounter : HUDElement() {

    override val name = "PP counter"

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)

    private var value = 0
        set(value) {
            if (field != value) {
                field = value
                updateText()
            }
        }

    private val counter = RollingDoubleCounter(0.0).apply {
        rollingDuration = 1000f
    }

    init {
        attachChild(sprite)
        updateText()
        onContentChanged()
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        counter.update(secondsElapsed * 1000)

        // The NaN check here is necessary as calculations may result in NaN values, which should not be displayed.
        // This is not a foolproof solution, but is enough to prevent crashes from happening.
        // Until the source of the NaN values is found and fixed, this must remain in place.
        counter.targetValue = game.stat.pp.takeIf { !it.isNaN() } ?: 0.0
        value = counter.currentValue.roundToInt()
    }

    private fun updateText() {
        sprite.text = "${value}${if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid) "dpp" else "pp"}"
    }
}