package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.mods.Mod

/**
 * Used to processes strain values of [StandardDifficultyHitObject]s, keep track of strain levels caused by
 * the processed objects and to calculate a final difficulty value representing the difficulty of
 * hitting all the processed objects.
 */
abstract class StandardStrainSkill(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>
) : StrainSkill<StandardDifficultyHitObject>(mods) {
    /**
     * The weight by which each strain value decays.
     */
    protected open val decayWeight = 0.9

    override fun difficultyValue() = currentStrainPeaks.run {
        // We are reducing the highest strains first to account for extreme difficulty spikes.
        reduceHighestStrainPeaks(this)

        // Difficulty is the weighted sum of the highest strains from every section.
        // We're sorting from highest to lowest strain.
        sortDescending()

        difficulty = 0.0
        var weight = 1.0

        for (strain in this) {
            difficulty += strain * weight
            weight *= decayWeight
        }

        difficulty
    }
}