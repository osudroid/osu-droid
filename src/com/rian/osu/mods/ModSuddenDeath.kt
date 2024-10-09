package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Sudden Death mod.
 */
class ModSuddenDeath : Mod(), IModUserSelectable {
    override val droidChar = 'u'
    override val acronym = "SD"
    override val textureNameSuffix = "suddendeath"
    override val enum = GameMod.MOD_SUDDENDEATH
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModPerfect::class, ModAuto::class
    )

    override fun equals(other: Any?) = other === this || other is ModSuddenDeath
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModSuddenDeath()
}