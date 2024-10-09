package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Precise mod.
 */
class ModPrecise : Mod(), IModUserSelectable {
    override val droidChar = 's'
    override val acronym = "PR"
    override val textureNameSuffix = "precise"
    override val enum = GameMod.MOD_PRECISE
    override val isRanked = true

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun equals(other: Any?) = other === this || other is ModPrecise
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModPrecise()
}