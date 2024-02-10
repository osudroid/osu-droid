package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.math.Vector2

/**
 * Represents the head of a slider.
 */
class SliderHead(
    /**
     * The start time of this slider head, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this slider head relative to the play field.
     */
    position: Vector2
) : SliderHitObject(startTime, position, 0, startTime) {
    override fun clone() = super.clone() as SliderHead
}
