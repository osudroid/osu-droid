package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * Represents a hit circle.
 */
class HitCircle(
    /**
     * The start time of this [HitCircle], in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [HitCircle] relative to the play field.
     */
    position: Vector2,

    /**
     * Whether this [HitCircle] starts a new combo.
     */
    isNewCombo: Boolean = false,

    /**
     * How many combo colors to skip, if this [HitCircle] starts a new combo.
     */
    comboColorOffset: Int = 0
) : HitObject(startTime, position, isNewCombo, comboColorOffset) {
    override fun clone() = super.clone() as HitCircle
}
