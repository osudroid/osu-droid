package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow

/**
 * Used to processes strain values of [DroidDifficultyHitObject]s, keep track of strain levels caused by
 * the processed objects and to calculate a final difficulty value representing the difficulty of
 * hitting all the processed objects.
 */
abstract class DroidStrainSkill(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>
) : StrainSkill<DroidDifficultyHitObject>(mods) {
    /**
     * The bonus multiplier that is given for a sequence of notes of equal difficulty.
     */
    protected abstract val starsPerDouble: Double

    /**
     * The strain of the currently calculated [DroidDifficultyHitObject].
     */
    protected abstract val objectStrain: Double

    /**
     * All [DroidDifficultyHitObject]s strains.
     */
    val objectStrains = mutableListOf<Double>()

    /**
     * Returns the number of strains weighed against the top strain.
     *
     * The result is scaled by clock rate as it affects the total number of strains.
     */
    fun countDifficultStrains() = objectStrains.run {
        if (isEmpty()) {
            return@run 0.0
        }

        val maxStrain = max()
        if (maxStrain == 0.0) {
            return@run 0.0
        }

        reduce { acc, d -> acc + (d / maxStrain).pow(4) }
    }

    override fun process(current: DroidDifficultyHitObject) {
        super.process(current)

        objectStrains.add(objectStrain)
    }

    override fun difficultyValue() = currentStrainPeaks.run {
        if (reducedSectionCount > 0) {
            sortDescending()

            // We are reducing the highest strains first to account for extreme difficulty spikes.
            for (i in 0 until min(size, reducedSectionCount)) {
                val scale = log10(
                    Interpolation.linear(
                        1.0,
                        10.0,
                        (i.toFloat() / reducedSectionCount).toDouble().coerceIn(0.0, 1.0)
                    )
                )

                this[i] *= Interpolation.linear(reducedSectionBaseline, 1.0, scale)
            }
        }

        // Math here preserves the property that two notes of equal difficulty x, we have their summed difficulty = x * starsPerDouble.
        // This also applies to two sets of notes with equal difficulty.
        val starsPerDoubleLog2 = log2(starsPerDouble)
        reduce { acc, d -> acc + d.pow(1 / starsPerDoubleLog2) }.pow(starsPerDoubleLog2)
    }

    override fun calculateCurrentSectionStart(current: DroidDifficultyHitObject) = current.startTime
}