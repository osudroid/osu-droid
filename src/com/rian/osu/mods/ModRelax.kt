package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Relax mod.
 */
class ModRelax : Mod(), IModUserSelectable {
    override val droidString = "x"
    override val acronym = "RX"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModAuto::class, ModNoFail::class, ModAutopilot::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1e-3f
}