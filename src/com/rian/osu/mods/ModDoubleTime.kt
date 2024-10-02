package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : Mod(), IModApplicableToTrackRate {
    override val droidString = "d"
    override val acronym = "DT"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNightCore::class, ModHalfTime::class
    )

    override val trackRateMultiplier = 1.5f

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.12f
}