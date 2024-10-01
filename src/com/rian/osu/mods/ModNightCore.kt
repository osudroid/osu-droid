package com.rian.osu.mods

/**
 * Represents the Night Core mod.
 */
class ModNightCore : Mod(), IModApplicableToTrackRate {
    override val droidString = "c"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModDoubleTime::class, ModHalfTime::class
    )

    override val trackRateMultiplier = 1.5f
}