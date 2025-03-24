package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : ModRateAdjust(0.75f), IModUserSelectable {
    override val encodeChar = 't'
    override val acronym = "HT"
    override val textureNameSuffix = "halftime"
    override val enum = GameMod.MOD_HALFTIME
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModDoubleTime::class, ModNightCore::class)

    override fun equals(other: Any?) = other === this || other is ModHalfTime
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModHalfTime()
}