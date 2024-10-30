package com.rian.osu.mods

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "t"

    override fun applyToRate(rate: Float, oldStatistics: Boolean) = rate * 0.75f
}