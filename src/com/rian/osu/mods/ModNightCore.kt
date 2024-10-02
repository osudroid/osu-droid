package com.rian.osu.mods

/**
 * Represents the Night Core mod.
 */
class ModNightCore : ModRateAdjust(), IModUserSelectable {
    override val droidString = "c"
    override val acronym = "NC"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModDoubleTime::class, ModHalfTime::class
    )

    override val trackRateMultiplier = 1.5f

    override fun equals(other: Any?) = other === this || other is ModNightCore
    override fun hashCode() = super.hashCode()
}