package com.reco1l.osu.hud.elements

import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.playfield.*
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.*

@Suppress("LeakingThis")
sealed class HUDStatisticCounter(
    label: String,
    valueColor: ColorARGB,
    private val dataSupplier: StatisticV2.() -> String
) : HUDElement() {

    private val labelText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = label
    }

    private val valueText = SpriteFont(OsuSkin.get().scorePrefix).apply {
        color = valueColor
    }


    init {
        attachChild(labelText)
        attachChild(valueText)
    }


    override fun onGameplayUpdate(gameScene: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        valueText.text = statistics.dataSupplier()
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        valueText.y = labelText.drawHeight
        super.onManagedUpdate(pSecondsElapsed)
    }
}


class HUDGreatCounter : HUDStatisticCounter("Great", ColorARGB(0xFF46b4dc),  { hit300.toString() })
class HUDOkCounter : HUDStatisticCounter("Ok", ColorARGB(0xFF64DC28), { hit100.toString() })
class HUDMehCounter : HUDStatisticCounter("Meh", ColorARGB(0xFFc8b46e), { hit50.toString() })
class HUDMissCounter : HUDStatisticCounter("Miss", ColorARGB.Red, { misses.toString() })