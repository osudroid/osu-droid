package com.reco1l.osu.hud.elements

import com.reco1l.andengine.text.ExtendedText
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.playfield.SpriteFont
import com.reco1l.osu.updateThread
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin

class HUDTapsPerSecondCounter : HUDElement() {

    override val name = "Taps per second counter"

    private val label = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = "Taps/sec"
    }

    private val value = SpriteFont(OsuSkin.get().scorePrefix).apply {
        text = "0"
    }

    private val timestamps = ArrayDeque<Float>(50)

    init {
        attachChild(label)
        attachChild(value)
    }

    override fun onGameplayTouchDown(time: Float) {
        // Queue to the update thread to avoid the timestamp queue potentially resizing itself in the main thread.
        updateThread { timestamps.add(time) }
    }

    override fun onGameplayUpdate(gameScene: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        val earliestElapsedTime = gameScene.elapsedTime - GameHelper.getSpeedMultiplier()

        while (timestamps.isNotEmpty() && timestamps.first() < earliestElapsedTime) {
            timestamps.removeFirst()
        }

        value.text = timestamps.size.toString()
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        value.y = label.drawHeight
        super.onManagedUpdate(pSecondsElapsed)
    }
}