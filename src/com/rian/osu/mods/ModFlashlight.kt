package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Flashlight mod.
 */
class ModFlashlight : Mod(), IModUserSelectable {
    override val droidString = "i"
    override val acronym = "FL"
    override val ranked = true

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.12f
}