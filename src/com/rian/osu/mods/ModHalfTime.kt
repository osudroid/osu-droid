package com.rian.osu.mods

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "t"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModDoubleTime::class, ModNightCore::class
    )

    override val trackRateMultiplier = 0.75f
}