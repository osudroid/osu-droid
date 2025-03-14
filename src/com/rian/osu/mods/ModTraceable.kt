package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Traceable mod.
 */
class ModTraceable : Mod(), IModUserSelectable {
    override val droidChar = 'b'
    override val acronym = "TC"
    override val enum = GameMod.MOD_TRACEABLE
    override val textureNameSuffix = "traceable"

    override fun equals(other: Any?) = other === this || other is ModTraceable
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModTraceable()
}