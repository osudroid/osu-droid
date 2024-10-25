package com.rian.osu.difficulty.attributes

import com.rian.osu.mods.Mod

/**
 * Holds data that can be used to calculate osu!standard performance points.
 */
class StandardDifficultyAttributes : DifficultyAttributes() {
    /**
     * The difficulty corresponding to the speed skill.
     */
    @JvmField
    var speedDifficulty = 0.0

    /**
     * The amount of strains that are considered difficult with respect to the speed skill.
     */
    @JvmField
    var speedDifficultStrainCount = 0.0

    /**
     * The perceived approach rate inclusive of rate-adjusting [Mod]s (DT/HT/etc.).
     *
     * Rate-adjusting [Mod]s don't directly affect the approach rate difficulty value, but have a perceived effect as a result of adjusting audio timing.
     */
    @JvmField
    var approachRate = 0.0
}