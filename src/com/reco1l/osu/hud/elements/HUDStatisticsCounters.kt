package com.reco1l.osu.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.playfield.*
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.*

@Suppress("LeakingThis")
sealed class HUDStatisticCounter(label: String) : HUDElement() {

    protected val labelText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = label
    }

    protected val valueText = SpriteFont(OsuSkin.get().scorePrefix)

    init {
        attachChild(labelText)
        attachChild(valueText)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        valueText.y = labelText.drawHeight

        val innerAnchor = when(anchor.x) {
            0f -> Anchor.TopLeft
            0.5f -> Anchor.TopCenter
            else -> Anchor.TopRight
        }

        labelText.anchor = innerAnchor
        labelText.origin = innerAnchor
        valueText.anchor = innerAnchor
        valueText.origin = innerAnchor

        super.onManagedUpdate(pSecondsElapsed)
    }
}

sealed class HUDHitStatisticCounter(
    label: String,
    tint: ColorARGB,
    private val dataSupplier: StatisticV2.() -> String
) : HUDStatisticCounter(label) {

    init {
        labelText.color = tint
        valueText.color = tint
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        valueText.text = game.stat.dataSupplier()
    }
}

class HUDGreatCounter : HUDHitStatisticCounter("Great", ColorARGB(0xFF46b4dc),  { hit300.toString() })
class HUDOkCounter : HUDHitStatisticCounter("Ok", ColorARGB(0xFF64DC28), { hit100.toString() })
class HUDMehCounter : HUDHitStatisticCounter("Meh", ColorARGB(0xFFc8b46e), { hit50.toString() })
class HUDMissCounter : HUDHitStatisticCounter("Miss", ColorARGB.Red, { misses.toString() })