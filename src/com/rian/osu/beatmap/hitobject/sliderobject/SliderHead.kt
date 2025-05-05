package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.math.Vector2

/**
 * Represents the head of a [Slider].
 */
class SliderHead(
    /**
     * The start time of this [SliderHead], in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [SliderHead] relative to the play field.
     */
    position: Vector2
) : SliderHitObject(startTime, position)
