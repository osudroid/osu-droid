package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.math.Vector2

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
    position: Vector2,

    /**
     * The index of the span at which this [SliderHitObject] lies.
     */
    spanIndex: Int,

    /**
     * The start time of the span at which this [SliderHitObject] lies, in milliseconds.
     */
    spanStartTime: Double
) : HitObject(startTime, position) {
    /**
     * The index of the span at which this [SliderHitObject] lies.
     */
    var spanIndex = spanIndex
        private set

    /**
     * The start time of the span at which this [SliderHitObject] lies, in milliseconds.
     */
    var spanStartTime = spanStartTime
        private set
}
