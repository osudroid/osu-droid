package com.rian.osu.beatmap.hitobject

/**
 * A [HitObject] that ends at a different time than its start time.
 */
interface IHasDuration {
    /**
     * The time at which this [IHasDuration] ends, in milliseconds.
     */
    val endTime: Double

    /**
     * The duration of this [IHasDuration], in milliseconds.
     */
    val duration: Double
}