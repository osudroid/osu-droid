package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : ModClockRateAdjust(), IModUserSelectable {
    override val droidChar = 't'
    override val acronym = "HT"
    override val textureNameSuffix = "halftime"
    override val enum = GameMod.MOD_HALFTIME
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModDoubleTime::class, ModNightCore::class
    )

    override val trackRateMultiplier = 0.75f

    override fun equals(other: Any?) = other === this || other is ModHalfTime
    override fun hashCode() = super.hashCode()
}