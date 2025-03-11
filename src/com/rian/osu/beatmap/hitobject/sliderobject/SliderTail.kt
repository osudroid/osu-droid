package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.beatmap.hitobject.Slider

/**
 * Represents the tail of a [Slider].
 */
class SliderTail(
    /**
     * The [Slider] to which this [SliderTail] belongs.
     */
    slider: Slider
) : SliderEndCircle(slider, slider.repeatCount)
