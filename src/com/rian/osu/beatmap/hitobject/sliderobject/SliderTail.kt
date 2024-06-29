package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.math.Vector2

/**
 * Represents a slider tail.
 */
class SliderTail(
    /**
     * The time at which this [SliderTail] starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [SliderTail] relative to the play field.
     */
    position: Vector2,

    /**
     * The index of the span at which this [SliderTail] lies.
     */
    spanIndex: Int,

    /**
     * The start time of the span at which this [SliderTail] lies, in milliseconds.
     */
    spanStartTime: Double
) : SliderHitObject(startTime, position, spanIndex, spanStartTime)
