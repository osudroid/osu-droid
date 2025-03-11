package com.reco1l.osu.hitobjects

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Modifiers
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.*
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.beatmap.hitobject.sliderobject.*
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class SliderTickContainer : Container() {
    private var slider: Slider? = null

    fun init(currentTimeSec: Double, beatmapSlider: Slider) {
        slider = beatmapSlider

        detachChildren()

        val position = beatmapSlider.gameplayStackedPosition

        setPosition(position.x, position.y)

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.gameplayStackedPosition

            val sprite = SliderTickSprite.pool.obtain()

            // We're subtracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.setPosition(tickPosition.x - position.x, tickPosition.y - position.y)
            sprite.init(currentTimeSec, tick)

            attachChild(sprite)
        }
    }

    fun onNewSpan(currentTimeSec: Double, newSpanIndex: Int) {
        if (slider == null) {
            return
        }

        val spanStartIndex =
            // Amount of slider ticks passed.
            newSpanIndex * childCount +
            // Amount of slider repeats passed.
            newSpanIndex +
            // The slider head.
            1

        for (i in spanStartIndex until spanStartIndex + childCount) {
            val tick = slider!!.nestedHitObjects[i] as? SliderTick ?: break

            // For reverse sliders, the ticks are in the opposite order.
            val sprite = getChild(
                if (newSpanIndex % 2 != 0) childCount - (i - spanStartIndex) - 1 else i - spanStartIndex
            ) as? SliderTickSprite ?: break

            sprite.init(currentTimeSec, tick)
        }
    }

    override fun onDetached() {
        super.onDetached()
        detachChildren()
    }

}


class SliderTickSprite : ExtendedSprite() {

    init {
        textureRegion = ResourceManager.getInstance().getTexture("sliderscorepoint")
        origin = Anchor.Center
    }

    /**
     * Initializes this [SliderTickSprite] with the given [SliderTick].
     *
     * @param currentTimeSec The current time in seconds.
     * @param tick The [SliderTick] represented by this [SliderTickSprite].
     */
    fun init(currentTimeSec: Double, tick: SliderTick) {
        val startTime = (tick.startTime / 1000).toFloat()
        val timePreempt = (tick.timePreempt / 1000).toFloat()

        val fadeInStartTime = startTime - timePreempt

        clearEntityModifiers()

        alpha = 0f
        setScale(0.5f)

        registerEntityModifier(
            Modifiers.sequence(null,
                Modifiers.delay(fadeInStartTime - currentTimeSec.toFloat()),
                Modifiers.parallel(null,
                    Modifiers.scale(ANIM_DURATION * 4, 0.5f, 1f, easing = Easing.OutElasticHalf),
                    Modifiers.fadeIn(ANIM_DURATION)
                )
            )
        )

        if (GameHelper.isHidden()) {
            val fadeOutDuration = min(timePreempt - ANIM_DURATION, 1f)
            val fadeOutStartTime = startTime - fadeOutDuration

            registerEntityModifier(
                Modifiers.sequence(null,
                    Modifiers.delay(fadeOutStartTime - currentTimeSec.toFloat()),
                    Modifiers.fadeOut(fadeOutDuration)
                )
            )
        }
    }

    /**
     * Called when the [SliderTick] that this [SliderTickSprite] represents is hit.
     *
     * @param isSuccessful Whether the hit resulted in a successful hit.
     */
    fun onHit(isSuccessful: Boolean) {
        clearEntityModifiers()

        registerEntityModifier(Modifiers.alpha(ANIM_DURATION, alpha, 0f, easing = Easing.OutQuint))

        if (isSuccessful) {
            registerEntityModifier(Modifiers.scale(ANIM_DURATION, 1f, 1.5f, easing = Easing.Out))
        }
    }

    override fun onDetached() {
        super.onDetached()
        clearEntityModifiers()
        pool.free(this)
    }

    companion object {
        private const val ANIM_DURATION = 0.15f

        @JvmStatic
        val pool = Pool { SliderTickSprite() }

    }

}
