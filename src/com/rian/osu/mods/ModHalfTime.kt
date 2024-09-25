package com.rian.osu.mods

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "t"
    override val trackRateMultiplier = 0.75f
}