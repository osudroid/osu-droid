package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "t"
    override val acronym = "HT"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModDoubleTime::class, ModNightCore::class
    )

    override val trackRateMultiplier = 0.75f

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.3f
}