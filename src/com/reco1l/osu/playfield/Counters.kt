package com.reco1l.osu.playfield

import androidx.annotation.*
import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.OnModifierFinished
import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.PP
import com.reco1l.osu.playfield.ScoreCounterMetric.Companion.SCORE
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*
import java.text.*
import java.util.*

class ScoreCounter : SpriteFont(OsuSkin.get().scorePrefix) {

    @ScoreCounterMetric
    var metric = SCORE
        set(value) {

            format = when (value) {
                SCORE -> DecimalFormat("00000000")
                PP -> DecimalFormat("0.00")

                else -> throw IllegalArgumentException("Invalid metric: $value")
            }

            field = value
        }


    private var format = DecimalFormat()


    init {
        setAnchor(Anchor.TopRight)
        setOrigin(Anchor.TopRight)
        setScale(0.96f)

        x = -10f
    }


    fun setValue(value: Any) {
        text = format.format(value)
    }

}

/**
 * Defines the metric to be used by the score counter.
 */
@IntDef(SCORE, PP)
annotation class ScoreCounterMetric {
    companion object {
        const val SCORE = 0
        const val PP = 1
    }
}


class AccuracyCounter : SpriteFont(OsuSkin.get().scorePrefix) {


    private val format = NumberFormat.getPercentInstance(Locale.US).also {
        it.maximumFractionDigits = 2
    }


    init {
        setAnchor(Anchor.TopRight)
        setOrigin(Anchor.TopRight)
        setScale(0.6f * 0.96f)
        setPosition(-17f, characters['0']!!.height + 9f)

        x = -17f
        y = characters['0']!!.height + 9f

        text = "000.00%"
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

        attachChild(it)

    } else null

    private val displayedCountTextSprite = SpriteFont(OsuSkin.get().comboPrefix).also {

        it.text = "0x"
        it.setAnchor(Anchor.BottomLeft)
        it.setOrigin(Anchor.BottomLeft)

        it.translationY = -(FONT_HEIGHT_RATIO * it.height + VERTICAL_OFFSET)

        it.y = -(1 - FONT_HEIGHT_RATIO) * it.height + VERTICAL_OFFSET

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