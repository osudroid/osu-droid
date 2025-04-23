package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Traceable mod.
 */
class ModTraceable : Mod() {
    override val name = "Traceable"
    override val acronym = "TC"
    override val description = "Put your faith in the approach circles..."
    override val type = ModType.DifficultyIncrease
    override val incompatibleMods = super.incompatibleMods + ModHidden::class

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun equals(other: Any?) = other === this || other is ModTraceable
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModTraceable()
}