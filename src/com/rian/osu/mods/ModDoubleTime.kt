package com.rian.osu.mods

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : ModRateAdjust(), IModUserSelectable {
    override val droidString = "d"
    override val acronym = "DT"
    override val textureNameSuffix = "doubletime"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNightCore::class, ModHalfTime::class
    )

    override val trackRateMultiplier = 1.5f

    override fun equals(other: Any?) = other === this || other is ModDoubleTime
    override fun hashCode() = super.hashCode()
}