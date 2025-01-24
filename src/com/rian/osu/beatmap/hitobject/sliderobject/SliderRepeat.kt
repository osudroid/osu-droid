package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.beatmap.hitobject.Slider

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
