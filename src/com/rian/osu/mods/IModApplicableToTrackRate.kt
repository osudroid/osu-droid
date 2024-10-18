package com.rian.osu.mods

/**
 * An interface for [Mod]s that make adjustments to the track's playback rate.
 */
interface IModApplicableToTrackRate {
    /**
     * Returns the playback rate with this [IModApplicableToTrackRate] applied.
     *
     * @param rate The rate to apply this [IModApplicableToTrackRate] to.
     * @param oldStatistics Whether to enforce old statistics. Some [IModApplicableToTrackRate]s behave differently
     * with this flag. For example, [ModNightCore] will apply a 1.39 rate multiplier instead of 1.5 when this is `true`.
     * **Never set this flag to `true` unless you know what you are doing.**
     */
    fun applyToRate(rate: Float, oldStatistics: Boolean = false): Float
}