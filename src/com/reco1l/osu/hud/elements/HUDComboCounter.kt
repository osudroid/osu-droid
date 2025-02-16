package com.reco1l.osu.hud.elements

import com.edlplan.framework.easing.*
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.modifier.OnModifierFinished
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.playfield.SpriteFont
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.*


class HUDComboCounter : HUDElement() {


    private val popOutCount = if (Config.isAnimateComboText()) SpriteFont(OsuSkin.get().comboPrefix).also {

        it.alpha = 0f
        it.text = "0x"
        it.spacing = -OsuSkin.get().comboOverlap

        attachChild(it)

    } else null

    private val displayedCountTextSprite = SpriteFont(OsuSkin.get().comboPrefix).also {

        it.text = "0x"
        it.spacing = -OsuSkin.get().comboOverlap

        attachChild(it, 0)
    }

    private val updateDisplayedCount = OnModifierFinished {
        displayedCountTextSprite.text = "${current}x"
    }


    private var current = 0


    init {
        onMeasureContentSize()
    }


    fun setCombo(value: Int) {

        if (current == value) {
            return
        }

        // Means the animation is disabled by the user.
        if (popOutCount == null) {
            current = value
            displayedCountTextSprite.text = "${current}x"
            return
        }

        popOutCount.clearEntityModifiers()
        displayedCountTextSprite.clearEntityModifiers()

        // Force update the text to the current combo if the previous
        // modifier haven't finished yet.
        current = value
        updateDisplayedCount(popOutCount)

        popOutCount.text = "${current}x"
        popOutCount.alpha = 0.6f
        popOutCount.setScale(1.56f)

        popOutCount.scaleTo(1f, BIG_POP_OUT_DURATION)
        popOutCount.fadeOut(BIG_POP_OUT_DURATION).then(updateDisplayedCount)

        displayedCountTextSprite.setScale(1f)

        displayedCountTextSprite
            .scaleTo(1.1f, SMALL_POP_OUT_DURATION / 2f, Easing.In)
            .scaleTo(1f, SMALL_POP_OUT_DURATION / 2f, Easing.Out)
    }


    override fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        setCombo(statistics.combo)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        popOutCount?.origin = anchor
        displayedCountTextSprite.origin = anchor


        super.onManagedUpdate(pSecondsElapsed)
    }


    companion object {
        const val BIG_POP_OUT_DURATION = 0.3f
        const val SMALL_POP_OUT_DURATION = 0.1f
    }

}