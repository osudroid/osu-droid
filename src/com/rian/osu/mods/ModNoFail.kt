package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the No Fail mod.
 */
class ModNoFail : Mod() {
    override val droidString = "n"
    override val acronym = "NF"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModPerfect::class, ModSuddenDeath::class, ModAutopilot::class, ModRelax::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.5f
}