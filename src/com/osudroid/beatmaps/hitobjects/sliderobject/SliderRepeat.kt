package com.osudroid.beatmaps.hitobjects.sliderobject

import com.osudroid.beatmaps.hitobjects.Slider

/**
 * Represents a slider repeat.
 */
class SliderRepeat(
    /**
     * The slider to which this [SliderRepeat] belongs.
     */
    slider: Slider,

    /**
     * The index of the span at which this [SliderRepeat] lies.
     */
    spanIndex: Int
) : SliderEndCircle(slider, spanIndex)
