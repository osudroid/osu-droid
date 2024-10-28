package com.reco1l.osu.playfield

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.OnModifierFinished
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*
import java.text.*
import java.util.*
import kotlin.math.roundToInt

class ScoreCounter : SpriteFont(OsuSkin.get().scorePrefix) {


    private val format = DecimalFormat("00000000", DecimalFormatSymbols(Locale.US))


    init {
        setAnchor(Anchor.TopRight)
        setOrigin(Anchor.TopRight)
        setScale(0.96f)

        x = -10f
        spacing = -OsuSkin.get().scoreOverlap
    }


    fun setScore(value: Int) {
        text = format.format(value)
    }

}

class PPCounter(private val algorithm: DifficultyAlgorithm) : SpriteFont(OsuSkin.get().scorePrefix) {

    init {
        setAnchor(Anchor.TopRight)
        setOrigin(Anchor.TopRight)
        setScale(0.6f * 0.96f)
        setValue(0.0)
    }


    fun setValue(value: Double) {
        text = "${value.roundToInt()}${if (algorithm == DifficultyAlgorithm.droid) "dpp" else "pp"}"
    }
}


class AccuracyCounter : SpriteFont(OsuSkin.get().scorePrefix) {


    private val format = DecimalFormat("0.00%", DecimalFormatSymbols(Locale.US))


    init {
        setAnchor(Anchor.TopRight)
        setOrigin(Anchor.TopRight)
        setScale(0.6f * 0.96f)
        setPosition(-17f, 9f)
        text = "100.00%"
    }


    fun setAccuracy(value: Float) {
        text = format.format(value)
    }

}


class ComboCounter : Container() {


    private val popOutCount = if (Config.isAnimateComboText()) SpriteFont(OsuSkin.get().comboPrefix).also {

        it.alpha = 0f
        it.text = "0x"
        it.setAnchor(Anchor.BottomLeft)
        it.setOrigin(Anchor.BottomLeft)

        // In stable, the bigger pop out scales a bit to the left
        it.translationX = -3f
        it.translationY = -(FONT_HEIGHT_RATIO * it.height + VERTICAL_OFFSET)

        it.y = -(1 - FONT_HEIGHT_RATIO) * it.height + VERTICAL_OFFSET
        it.spacing = -OsuSkin.get().comboOverlap

        attachChild(it)

    } else null

    private val displayedCountTextSprite = SpriteFont(OsuSkin.get().comboPrefix).also {

        it.text = "0x"
        it.setAnchor(Anchor.BottomLeft)
        it.setOrigin(Anchor.BottomLeft)

        it.translationY = -(FONT_HEIGHT_RATIO * it.height + VERTICAL_OFFSET)

        it.y = -(1 - FONT_HEIGHT_RATIO) * it.height + VERTICAL_OFFSET
        it.spacing = -OsuSkin.get().comboOverlap

        attachChild(it)
    }

    private val updateDisplayedCount = OnModifierFinished {
        displayedCountTextSprite.text = "${current}x"
    }


    private var current = 0


    init {
        setAnchor(Anchor.BottomLeft)
        setOrigin(Anchor.BottomLeft)
        setPosition(10f, -10f)
        setScale(1.28f)
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
            .scaleTo(1.1f, SMALL_POP_OUT_DURATION / 2f).eased(Easing.In)
            .scaleTo(1f, SMALL_POP_OUT_DURATION / 2f).eased(Easing.Out)
    }


    companion object {

        const val FONT_HEIGHT_RATIO = 0.625f

        const val VERTICAL_OFFSET = 9f

        const val BIG_POP_OUT_DURATION = 0.3f

        const val SMALL_POP_OUT_DURATION = 0.1f

    }

}