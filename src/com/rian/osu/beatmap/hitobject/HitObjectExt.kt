@file:JvmName("HitObjectUtils")

package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode

/**
 * The end time of this hit object.
 */
fun HitObject.getEndTime() = if (this is IHasDuration) this.endTime else this.startTime

/**
 * The end position of this hit object.
 */
fun HitObject.getEndPosition() = if (this is Slider) this.endPosition else this.position

/**
 * Gets the stacked end position of this hit object.
 *
 * @param mode The [GameMode] to get.
 */
fun HitObject.getStackedEndPosition(mode: GameMode) =
    if (this is Slider) this.getStackedEndPosition(mode)
    else this.getStackedPosition(mode)