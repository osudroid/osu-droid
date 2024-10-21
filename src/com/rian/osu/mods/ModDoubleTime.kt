package com.rian.osu.mods

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "d"

    override fun applyToRate(rate: Float, oldStatistics: Boolean) = rate * 1.5f
}