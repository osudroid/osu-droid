package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.math.Vector2

/**
 * Represents a slider tick.
 */
class SliderTick(
    /**
     * The time at which this [SliderTick] starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [SliderTick] relative to the play field.
     */
    position: Vector2,

    /**
     * The index of the span at which this [SliderTick] lies.
     */
    spanIndex: Int,

    /**
     * The start time of the span at which this [SliderTick] lies, in milliseconds.
     */
    spanStartTime: Double
) : SliderHitObject(startTime, position, spanIndex, spanStartTime)
