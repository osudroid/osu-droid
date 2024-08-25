@file:JvmName("HitObjectUtils")

package com.rian.osu.beatmap.hitobject

/**
 * The end time of this hit object.
 */
fun HitObject.getEndTime() = if (this is IHasDuration) this.endTime else this.startTime

/**
 * The end position of this hit object.
 */
fun HitObject.getEndPosition() = if (this is Slider) this.endPosition else this.position

/**
 * The gameplay end position of this hit object.
 */
fun HitObject.getGameplayEndPosition() = if (this is Slider) this.gameplayEndPosition else this.gameplayPosition

/**
 * The difficulty stacked end position of this hit object.
 */
fun HitObject.getDifficultyStackedEndPosition() = if (this is Slider) this.difficultyStackedEndPosition else this.difficultyStackedPosition

/**
 * The gameplay stacked end position of this hit object.
 */
fun HitObject.getGameplayStackedEndPosition() = if (this is Slider) this.gameplayStackedEndPosition else this.gameplayStackedPosition