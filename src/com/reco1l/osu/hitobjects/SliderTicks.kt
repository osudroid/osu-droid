package com.reco1l.osu.hitobjects

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.*
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.beatmap.hitobject.sliderobject.*
import ru.nsu.ccfit.zuev.osu.*

class SliderTickContainer : Container() {

    fun init(beatmapSlider: Slider) {

        detachChildren()

        val position = beatmapSlider.gameplayStackedPosition

        for (i in 1 until beatmapSlider.nestedHitObjects.size) {

            val tick = beatmapSlider.nestedHitObjects[i] as? SliderTick ?: break
            val tickPosition = tick.gameplayStackedPosition

            val sprite = SliderTickSprite.pool.obtain()
            sprite.alpha = 0f

            // We're substracting the position of the slider because the tick container is
            // already at the position of the slider since it's a child of the slider's body.
            sprite.setPosition(tickPosition.x - position.x, tickPosition.y - position.y)

            attachChild(sprite)
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

    override fun onDetached() {
        super.onDetached()
        pool.free(this)
    }

    companion object {

        @JvmStatic
        val pool = Pool { SliderTickSprite() }

    }

}
