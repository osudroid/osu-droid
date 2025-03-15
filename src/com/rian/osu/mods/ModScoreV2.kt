package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the ScoreV2 mod.
 */
class ModScoreV2 : Mod(), IModUserSelectable {
    override val droidChar = 'v'
    override val acronym = "V2"
    override val textureNameSuffix = "scorev2"
    override val enum = GameMod.MOD_SCOREV2
    override val isValidForMultiplayerAsFreeMod = false

    override fun equals(other: Any?) = other === this || other is ModScoreV2
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModScoreV2()
}