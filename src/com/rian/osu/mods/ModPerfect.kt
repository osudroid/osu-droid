package com.rian.osu.mods

/**
 * Represents the Perfect mod.
 */
class ModPerfect : Mod(), IModUserSelectable {
    override val droidString = "f"
    override val acronym = "PF"
    override val textureNameSuffix = "perfect"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModSuddenDeath::class, ModAuto::class
    )

    override fun equals(other: Any?) = other === this || other is ModPerfect
    override fun hashCode() = super.hashCode()
}