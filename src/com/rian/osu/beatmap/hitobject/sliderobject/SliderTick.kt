package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.math.Vector2

/**
 * Represents a slider tick.
 */
class SliderTick(
    /**
     * The time at which this slider tick starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of the slider tick relative to the play field.
     */
    position: Vector2,

    /**
     * The index of the span at which this slider hit object lies.
     */
    spanIndex: Int,

    /**
     * The start time of the span at which this slider hit object lies, in milliseconds.
     */
    spanStartTime: Double
) : SliderHitObject(startTime, position, spanIndex, spanStartTime) {
    override fun clone() = super.clone() as SliderTick
}
