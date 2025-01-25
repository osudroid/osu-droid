package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.beatmap.hitobject.Slider

/**
 * Represents a slider tail.
 */
class SliderTail(
    /**
     * The [Slider] to which this [SliderTail] belongs.
     */
    slider: Slider,

    /**
     * An optional start time for this [SliderTail] to override this [SliderTail]'s [startTime].
     *
     * Used for osu!standard's legacy slider tail.
     */
    startTime: Double = slider.endTime
) : SliderEndCircle(slider, slider.repeatCount, startTime)
