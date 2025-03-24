package com.rian.osu.mods

/**
 * An interface for [Mod]s that make adjustments to the track's playback rate.
 */
interface IModApplicableToTrackRate {
    /**
     * Returns the playback rate at [time] after this [Mod] is applied.
     *
     * @param time The time at which the playback rate is queried, in milliseconds.
     * @param rate The playback rate before applying this [Mod].
     * @return The playback rate after applying this [Mod].
     */
    fun applyToRate(time: Double, rate: Float = 1f): Float
}