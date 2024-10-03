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

    /**
     * The amount of seconds until the flashlight reaches the cursor.
     */
    var followDelay = DEFAULT_FOLLOW_DELAY

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.12f

    override fun equals(other: Any?) = other === this || other is ModFlashlight

    override fun hashCode(): Int {
        var result = super.hashCode()

        result = 31 * result + followDelay.hashCode()

        return result
    }

    companion object {
        /**
         * The default amount of seconds until the flashlight reaches the cursor.
         */
        const val DEFAULT_FOLLOW_DELAY = 0.12f
    }
}