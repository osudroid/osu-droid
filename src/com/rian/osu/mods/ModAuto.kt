package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Auto mod.
 */
class ModAuto : Mod(), IModUserSelectable {
    override val droidChar = 'a'
    override val acronym = "AT"
    override val textureNameSuffix = "autoplay"
    override val enum = GameMod.MOD_AUTO
    override val isValidForMultiplayer = false
    override val isValidForMultiplayerAsFreeMod = false
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutopilot::class, ModPerfect::class, ModSuddenDeath::class
    )

    override fun equals(other: Any?) = other === this || other is ModAuto
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModAuto()
}
