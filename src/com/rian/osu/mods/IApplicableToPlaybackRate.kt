package com.rian.osu.mods

/**
 * An interface for [Mod]s that make adjustments to the track's playback rate.
 */
interface IApplicableToPlaybackRate {
    /**
     * The multiplier to apply to the track's playback rate.
     */
    val trackRateMultiplier: Float
}