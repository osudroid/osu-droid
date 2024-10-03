package com.rian.osu.mods

/**
 * Represents the Sudden Death mod.
 */
class ModSuddenDeath : Mod(), IModUserSelectable {
    override val droidString = "u"
    override val acronym = "SD"
    override val textureNameSuffix = "suddendeath"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModPerfect::class, ModAuto::class
    )

    override fun equals(other: Any?) = other === this || other is ModSuddenDeath
    override fun hashCode() = super.hashCode()
}