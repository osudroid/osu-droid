package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Precise mod.
 */
class ModPrecise : Mod(), IModUserSelectable {
    override val droidString = "s"
    override val acronym = "PR"
    override val ranked = true

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun equals(other: Any?) = other === this || other is ModPrecise
    override fun hashCode() = super.hashCode()
}