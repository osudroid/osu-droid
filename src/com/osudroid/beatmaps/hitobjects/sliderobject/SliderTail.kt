package com.osudroid.beatmaps.hitobjects.sliderobject

import com.osudroid.beatmaps.hitobjects.Slider

/**
 * Represents the tail of a [Slider].
 */
class SliderTail(
    /**
     * The [Slider] to which this [SliderTail] belongs.
     */
    slider: Slider
) : SliderEndCircle(slider, slider.repeatCount)
