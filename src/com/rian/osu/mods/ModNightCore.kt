package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Night Core mod.
 */
open class ModNightCore : ModRateAdjust(1.5f), IModUserSelectable {
    override val encodeChar = 'c'
    override val acronym = "NC"
    override val textureNameSuffix = "nightcore"
    override val enum = GameMod.MOD_NIGHTCORE
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModDoubleTime::class, ModHalfTime::class)

    override fun equals(other: Any?) = other === this || other is ModNightCore
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModNightCore()
}