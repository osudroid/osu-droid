package com.reco1l.osu.hud.elements

import com.reco1l.osu.playfield.SpriteFont
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class HUDScoreCounter : HUDElement(tag = "scoreCounter") {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)
    private val format = DecimalFormat("00000000", DecimalFormatSymbols(Locale.US))

    init {
        sprite.spacing = -OsuSkin.get().scoreOverlap
        sprite.text = format.format(0)
        attachChild(sprite)
    }

    override fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        sprite.text = format.format(statistics.totalScoreWithMultiplier)
    }

}