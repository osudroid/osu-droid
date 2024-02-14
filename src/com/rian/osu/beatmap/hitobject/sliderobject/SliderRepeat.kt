package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.math.Vector2

/**
 * Represents a slider repeat.
 */
class SliderRepeat(
    /**
     * The time at which this [SliderRepeat] starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [SliderRepeat] relative to the play field.
     */
    position: Vector2,

    /**
     * The index of the span at which this [SliderRepeat] lies.
     */
    spanIndex: Int,

    /**
     * The start time of the span at which this [SliderRepeat] lies, in milliseconds.
     */
    spanStartTime: Double
) : SliderHitObject(startTime, position, spanIndex, spanStartTime)
