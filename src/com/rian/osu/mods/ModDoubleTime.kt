package com.rian.osu.mods

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "d"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNightCore::class, ModHalfTime::class
    )

    override val trackRateMultiplier = 1.5f
}