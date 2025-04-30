package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Autopilot mod.
 */
class ModAutopilot : Mod() {
    override val name = "Autopilot"
    override val acronym = "AP"
    override val description = "Automatic cursor movement - just follow the rhythm."
    override val type = ModType.Automation
    override val textureNameSuffix = "relax2"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutoplay::class, ModNoFail::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1e-3f
    override fun deepCopy() = ModAutopilot()
}