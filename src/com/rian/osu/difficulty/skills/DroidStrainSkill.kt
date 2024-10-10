package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.mods.Mod
import kotlin.math.log2
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

    override fun process(current: DroidDifficultyHitObject) {
        if (current.index < 0) {
            return
        }

        super.process(current)
    }

    override fun difficultyValue() = currentStrainPeaks.run {
        // We are reducing the highest strains first to account for extreme difficulty spikes.
        reduceHighestStrainPeaks(this)

        // Math here preserves the property that two notes of equal difficulty x, we have their summed difficulty = x * starsPerDouble.
        // This also applies to two sets of notes with equal difficulty.
        val starsPerDoubleLog2 = log2(starsPerDouble)

        difficulty = fold(0.0) { acc, strain ->
            acc + strain.pow(1 / starsPerDoubleLog2)
        }.pow(starsPerDoubleLog2)

        difficulty
    }

    override fun calculateCurrentSectionStart(current: DroidDifficultyHitObject) = current.startTime
}