package com.osudroid.beatmaps.hitobjects.sliderobject

import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.math.Vector2

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
