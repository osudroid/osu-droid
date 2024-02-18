package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import kotlin.math.log10
import kotlin.math.min

/**
 * Used to processes strain values of [DifficultyHitObject]s, keep track of strain levels caused by
 * the processed objects and to calculate a final difficulty value representing the difficulty of
 * hitting all the processed objects.
 */
abstract class OsuStrainSkill(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>
) : StrainSkill(mods) {
    /**
     * The final multiplier to be applied to the final difficulty value after all other calculations.
     */
    protected open val difficultyMultiplier = 1.06

    /**
     * The weight by which each strain value decays.
     */
    protected open val decayWeight = 0.9

    override fun difficultyValue(): Double {
        val strains = currentStrainPeaks

        // Difficulty is the weighted sum of the highest strains from every section.
        // We're sorting from highest to lowest strain.
        strains.sortDescending()

        if (reducedSectionCount > 0) {
            // We are reducing the highest strains first to account for extreme difficulty spikes.
            for (i in 0 until min(strains.size.toDouble(), reducedSectionCount.toDouble()).toInt()) {
                val scale = log10(
                    Interpolation.linear(
                        1.0,
                        10.0,
                        (i.toFloat() / reducedSectionCount).toDouble().coerceIn(0.0, 1.0)
                    )
                )

                strains[i] = strains[i] * Interpolation.linear(reducedSectionBaseline, 1.0, scale)
            }

            strains.sortDescending()
        }

        var difficulty = 0.0
        var weight = 1.0

        for (strain in strains) {
            difficulty += strain * weight
            weight *= decayWeight
        }

        return difficulty * difficultyMultiplier
    }
}