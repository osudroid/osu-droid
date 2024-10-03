package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Flashlight mod.
 */
class ModFlashlight : Mod(), IModUserSelectable {
    override val droidChar = 'i'
    override val acronym = "FL"
    override val textureNameSuffix = "flashlight"
    override val ranked = true

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.12f

    override fun equals(other: Any?) = other === this || other is ModFlashlight
    override fun hashCode() = super.hashCode()
}