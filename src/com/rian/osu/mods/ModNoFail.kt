package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the No Fail mod.
 */
class ModNoFail : Mod() {
    override val name = "No Fail"
    override val acronym = "NF"
    override val description = "You can't fail, no matter what."
    override val type = ModType.DifficultyReduction
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModPerfect::class, ModSuddenDeath::class, ModAutopilot::class, ModRelax::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.5f

    override fun deepCopy() = ModNoFail()
}