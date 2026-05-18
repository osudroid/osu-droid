package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.SpriteFont
import com.rian.framework.RollingFloatCounter
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class HUDAccuracyCounter : HUDElement() {

    private val sprite = SpriteFont(OsuSkin.get().scorePrefix)
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
        sprite.text = "100.00%"
        registerUpdateHandler(counter)
        attachChild(sprite)
        onContentChanged()
    }

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        counter.targetValue = gameScene.stat.accuracy
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        value = counter.currentValue

        super.onManagedUpdate(deltaTimeSec)
    }
}