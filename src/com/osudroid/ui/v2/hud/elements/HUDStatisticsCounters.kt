package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.SpriteFont
import com.reco1l.andengine.*
import com.reco1l.andengine.text.*
import com.reco1l.framework.*
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.andengine.theme.FontSize
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.*

@Suppress("LeakingThis")
sealed class HUDStatisticCounter(label: String) : HUDElement() {

    protected val labelText = UIText().apply {
        fontSize = FontSize.SM
        text = label
    }

    protected val valueText = SpriteFont(OsuSkin.get().scorePrefix)

    init {
        attachChild(labelText)
        attachChild(valueText)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        valueText.y = labelText.height

        val innerAnchor = when (anchor.x) {
            0f -> Anchor.TopLeft
            0.5f -> Anchor.TopCenter
            else -> Anchor.TopRight
        }

        labelText.anchor = innerAnchor
        labelText.origin = innerAnchor
        valueText.anchor = innerAnchor
        valueText.origin = innerAnchor

        super.onManagedUpdate(deltaTimeSec)
    }
}

sealed class HUDHitStatisticCounter(
    label: String,
    tint: Color4,
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

class HUDGreatCounter : HUDHitStatisticCounter("Great", Color4(0xFF46b4dc),  { hit300.toString() })
class HUDOkCounter : HUDHitStatisticCounter("Ok", Color4(0xFF64DC28), { hit100.toString() })
class HUDMehCounter : HUDHitStatisticCounter("Meh", Color4(0xFFc8b46e), { hit50.toString() })
class HUDMissCounter : HUDHitStatisticCounter("Miss", Color4.Red, { misses.toString() })