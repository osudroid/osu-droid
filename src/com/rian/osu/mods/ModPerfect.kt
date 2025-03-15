package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Perfect mod.
 */
class ModPerfect : Mod(), IModUserSelectable {
    override val droidChar = 'f'
    override val acronym = "PF"
    override val textureNameSuffix = "perfect"
    override val enum = GameMod.MOD_PERFECT
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModSuddenDeath::class, ModAuto::class
    )

    override fun equals(other: Any?) = other === this || other is ModPerfect
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModPerfect()
}