package com.rian.osu.mods

/**
 * An interface for [Mod]s that make adjustments to the track's playback rate.
 */
interface IModApplicableToTrackRate {
    /**
     * The multiplier to apply to the track's playback rate.
     */
    val trackRateMultiplier: Float
}