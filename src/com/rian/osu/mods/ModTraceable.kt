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
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModHidden::class, ModObjectScaleTween::class)

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f
    override fun deepCopy() = ModTraceable()
}