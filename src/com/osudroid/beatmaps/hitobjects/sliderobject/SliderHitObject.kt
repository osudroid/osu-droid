package com.osudroid.beatmaps.hitobjects.sliderobject

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.math.Vector2

/**
 * Represents a hit object that can be nested into a [Slider].
 */
abstract class SliderHitObject(
    /**
     * The time at which this [SliderHitObject] starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [SliderHitObject] relative to the play field.
     */
    position: Vector2
) : HitObject(startTime, position, false, 0)
