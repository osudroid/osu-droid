package com.reco1l.osu.hitobjects

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.Modifiers
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.*
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.beatmap.hitobject.sliderobject.*
import ru.nsu.ccfit.zuev.osu.*

class SliderTickContainer : Container() {
    private var slider: Slider? = null
    private val animDuration = 0.15f

    fun init(currentTimeSec: Double, beatmapSlider: Slider) {
        slider = beatmapSlider

        detachChildren()

        val position = beatmapSlider.gameplayStackedPosition

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.gameplayStackedPosition

            val sprite = SliderTickSprite.pool.obtain()

            // We're subtracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.setPosition(tickPosition.x - position.x, tickPosition.y - position.y)

            attachChild(sprite)
            applyTickAnimation(currentTimeSec, tick, sprite)
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

            applyTickAnimation(currentTimeSec, tick, sprite)
        }
    }

    private fun applyTickAnimation(currentTimeSec: Double, tick: SliderTick, sprite: SliderTickSprite) {
        val fadeInStartTime = (tick.startTime - tick.timePreempt) / 1000

        sprite.apply {
            clearEntityModifiers()

            alpha = 0f
            setScale(0.5f)

            registerEntityModifier(
                Modifiers.sequence(null,
                    // Delay up to fadeInStartTime
                    Modifiers.delay((fadeInStartTime - currentTimeSec).toFloat()),
                    Modifiers.parallel(null,
                        Modifiers.scale(animDuration * 4, 0.5f, 1f, easing = Easing.OutElasticHalf),
                        Modifiers.fadeIn(animDuration)
                    )
                )
            )
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
        originX = 0.5f
        originY = 0.5f
    }

    override fun onDetached() {
        super.onDetached()
        clearEntityModifiers()
        pool.free(this)
    }

    companion object {

        @JvmStatic
        val pool = Pool { SliderTickSprite() }

    }

}
