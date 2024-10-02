package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Autopilot mod.
 */
class ModAutopilot : Mod(), IModUserSelectable {
    override val droidString = "p"
    override val acronym = "AP"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAuto::class, ModNoFail::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1e-3f
}