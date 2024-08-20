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
    isNewCombo: Boolean,

    /**
     * When starting a new combo, the offset of the new combo relative to the current one.
     *
     * This is generally a setting provided by a beatmap creator to choreograph interesting color patterns
     * which can only be achieved by skipping combo colors with per-[HitObject] level.
     *
     * It is exposed via [HitObject.comboIndexWithOffsets].
     */
    comboOffset: Int
) : HitObject(startTime, position, isNewCombo, comboOffset)
