package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Traceable mod.
 */
class ModTraceable : Mod(), IModUserSelectable {
    override val encodeChar = 'b'
    override val acronym = "TC"
    override val enum = GameMod.MOD_TRACEABLE
    override val textureNameSuffix = "traceable"
    override val incompatibleMods = super.incompatibleMods + ModHidden::class

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun equals(other: Any?) = other === this || other is ModTraceable
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModTraceable()
}