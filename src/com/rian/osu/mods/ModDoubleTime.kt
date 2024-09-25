package com.rian.osu.mods

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "d"
    override val trackRateMultiplier = 1.5f
}