package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : ModClockRateAdjust(), IModUserSelectable {
    override val droidChar = 'd'
    override val acronym = "DT"
    override val textureNameSuffix = "doubletime"
    override val enum = GameMod.MOD_DOUBLETIME
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNightCore::class, ModHalfTime::class
    )

    override val trackRateMultiplier = 1.5f

    override fun equals(other: Any?) = other === this || other is ModDoubleTime
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModDoubleTime()
}