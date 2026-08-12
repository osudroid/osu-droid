package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.osudroid.ui.v2.SpriteFont
import com.reco1l.andengine.text.TextAlign
import com.rian.framework.RollingFloatCounter
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class HUDAccuracyCounter : HUDElement() {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix).apply {
        val digitRange = '0'..'9'
        val maxDigitWidth = digitRange.maxOfOrNull { characters[it]?.width?.toFloat() ?: 0f } ?: 0f

        fixedCharWidths = buildMap {
            for (c in digitRange) {
                put(c, maxDigitWidth)
            }

            characters['.']?.let { put('.', it.width.toFloat()) }
            characters['%']?.let { put('%', it.width.toFloat()) }
        }

        measureText = "100.00%"
        text = "100.00%"
    }

    private val format = DecimalFormat("0.00%", DecimalFormatSymbols(Locale.US))

    private var value = 0f
        set(value) {
            if (field != value) {
                field = value
                sprite.text = format.format(value)
            }
        }

    private val counter = RollingFloatCounter(1f).apply { rollingDuration = 1f }

    init {
        registerUpdateHandler(counter)
        attachChild(sprite)
        onContentChanged()
    }

    override fun setSkinData(data: HUDElementSkinData?) {
        super.setSkinData(data)

        sprite.textAlign = when {
            anchor.x < 0.5f -> TextAlign.Left
            anchor.x > 0.5f -> TextAlign.Right
            else -> TextAlign.Center
        }
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        counter.targetValue = gameScene.stat.accuracy
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        value = counter.currentValue

        super.onManagedUpdate(deltaTimeSec)
    }
}