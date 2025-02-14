package com.reco1l.osu.hud.elements

import com.reco1l.osu.playfield.SpriteFont
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class HUDAccuracyCounter : HUDElement(tag = "accuracyCounter") {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)
    private val format = DecimalFormat("0.00%", DecimalFormatSymbols(Locale.US))

    init {
        sprite.text = "100.00%"
        attachChild(sprite)
    }

    override fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        sprite.text = format.format(statistics.accuracy)
    }

}