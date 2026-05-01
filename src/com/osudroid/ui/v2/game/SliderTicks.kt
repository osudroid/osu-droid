package com.osudroid.ui.v2.game

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.modifier.Modifiers
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.*
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.beatmap.hitobject.sliderobject.*
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper

class SliderTickContainer : UIContainer() {
    private var slider: Slider? = null

    fun init(beatmapSlider: Slider) {
        slider = beatmapSlider
        val lifetimeStart = calculateLifetimeStart(0)

        detachChildren()

        val position = beatmapSlider.screenSpaceGameplayStackedPosition

        setPosition(position.x, position.y)

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.screenSpaceGameplayStackedPosition

            val sprite = SliderTickSprite.pool.obtain()

            // We're subtracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.setPosition(tickPosition.x - position.x, tickPosition.y - position.y)

            // It is safe to use lifetimeStart here as the ticks will be updated in GameplaySlider.updateAfterInit().
            sprite.init(tick, lifetimeStart, lifetimeStart)

            attachChild(sprite)
        }
    }

    fun onNewSpan(currentTimeSec: Float, newSpanIndex: Int) {
        if (slider == null) {
            return
        }

        val lifetimeStart = calculateLifetimeStart(newSpanIndex)

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

            sprite.init(tick, currentTimeSec, lifetimeStart)
        }
    }

    private fun calculateLifetimeStart(spanIndex: Int): Float {
        val slider = slider ?: return 0f

        if (spanIndex == 0) {
            return (slider.startTime - slider.timePreempt).toFloat() / 1000
        }

        return (slider.startTime + slider.spanDuration * spanIndex).toFloat() / 1000
    }


    override fun onDetached() {
        super.onDetached()
        detachChildren()
    }

}


class SliderTickSprite : UISprite() {

    init {
        textureRegion = ResourceManager.getInstance().getTexture("sliderscorepoint")
        origin = Anchor.Center
    }

    /**
     * Initializes this [SliderTickSprite] with the given [SliderTick].
     *
     * @param tick The [SliderTick] represented by this [SliderTickSprite].
     * @param currentTimeSec The current time in seconds.
     * @param spanLifetimeStart The lifetime start of the current [Slider] span, in seconds.
     */
    fun init(tick: SliderTick, currentTimeSec: Float, spanLifetimeStart: Float) {
        val startTime = (tick.startTime / 1000).toFloat()
        val timePreempt = (tick.timePreempt / 1000).toFloat()

        val fadeInStartTime = startTime - timePreempt
        val fadeInDelay = fadeInStartTime - spanLifetimeStart

        clearEntityModifiers()

        val dt = currentTimeSec - spanLifetimeStart

        alpha = 0f
        setScale(0.5f)

        registerEntityModifier(
            Modifiers.sequence(null,
                Modifiers.delay(fadeInDelay),
                Modifiers.parallel(null,
                    Modifiers.scale(ANIM_DURATION * 4, 0.5f, 1f, easing = Easing.OutElasticHalf),
                    Modifiers.fadeIn(ANIM_DURATION)
                )
            ).also {
                if (dt > 0) {
                    it.onUpdate(dt, this)
                }
            }
        )

        if (GameHelper.isHidden() && !GameHelper.getHidden().onlyFadeApproachCircles) {
            val fadeOutDuration = min(timePreempt - ANIM_DURATION, 1f)
            val fadeOutStartTime = startTime - fadeOutDuration
            val fadeOutDelay = fadeOutStartTime - spanLifetimeStart

            registerEntityModifier(
                Modifiers.sequence(null,
                    Modifiers.delay(fadeOutDelay),
                    Modifiers.fadeOut(fadeOutDuration)
                ).also {
                    if (dt > 0) {
                        it.onUpdate(dt, this)
                    }
                }
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
