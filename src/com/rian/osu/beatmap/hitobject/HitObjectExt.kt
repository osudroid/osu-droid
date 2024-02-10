@file:JvmName("HitObjectUtils")

package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * The end time of this hit object.
 */
fun HitObject.getEndTime() = if (this is IHasDuration) this.endTime else this.startTime

/**
 * The end position of this hit object.
 */
fun HitObject.getEndPosition() = if (this is Slider) this.endPosition else this.position

/**
 * The stacked end position of this hit object.
 */
fun HitObject.getStackedEndPosition() = if (this is Slider) this.stackedEndPosition else this.stackedPosition