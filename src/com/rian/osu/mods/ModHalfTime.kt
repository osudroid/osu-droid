package com.rian.osu.mods

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : Mod(), IApplicableToPlaybackRate {
    override val droidString = "t"
    override val trackRateMultiplier = 0.75f
}