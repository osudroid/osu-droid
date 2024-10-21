package com.rian.osu.mods

/**
 * Represents the Night Core mod.
 */
class ModNightCore : Mod(), IModApplicableToTrackRate {
    override val droidString = "c"

    override fun applyToRate(rate: Float, oldStatistics: Boolean) = rate * if (oldStatistics) 1.39f else 1.5f
}