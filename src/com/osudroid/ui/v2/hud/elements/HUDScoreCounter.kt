package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class HUDScoreCounter : HUDElement() {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)
    private val format = DecimalFormat("00000000", DecimalFormatSymbols(Locale.US))

    private var value = 0
        set(value) {
            if (field != value) {
                field = value
                sprite.text = format.format(value)
            }
        }

    init {
        sprite.spacing = -OsuSkin.get().scoreOverlap
        sprite.text = format.format(0)
        attachChild(sprite)

        onContentChanged()
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        value = game.stat.totalScoreWithMultiplier
    }

}