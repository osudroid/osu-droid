package com.rian.osu.mods

/**
 * Represents the Auto mod.
 */
class ModAuto : Mod(), IModUserSelectable {
    override val droidChar = 'a'
    override val acronym = "AT"
    override val textureNameSuffix = "autoplay"
    override val validForMultiplayerAsFreeMod = false
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutopilot::class, ModPerfect::class, ModSuddenDeath::class
    )

    override fun equals(other: Any?) = other === this || other is ModAuto
    override fun hashCode() = super.hashCode()
}
