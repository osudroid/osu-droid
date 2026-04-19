package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.mods.Mod
import kotlin.math.pow

/**
 * A skill that calculates the difficulty of [TObject]s using harmonic summation.
 */
abstract class HarmonicSkill<TObject : DifficultyHitObject>(mods: Iterable<Mod>) : Skill<TObject>(mods) {
    /**
     * The sum of note weights, calculated during summation.
     *
     * Required for any calculations that normalizes the difficulty value.
     */
    protected var noteWeightSum = 0.0
        private set

    /**
     * Scaling factor applied as `x / (i + 1)`, where `x` is the skill's [harmonicScale] and `i`
     * is the index of the [TObject] being processed.
     *
     * A higher value increases the influence of the hardest [TObject]s during summation.
     */
    protected open val harmonicScale = 1.0

    /**
     * An exponent that controls the rate of which decay increases as the index increases.
     *
     * Values closer to 1 decay faster, whilst lower values give more weight to easier [TObject]s.
     */
    protected open val decayExponent = 0.9

    override fun difficultyValue(): Double {
        if (objectDifficulties.isEmpty()) {
            return 0.0
        }

        // Notes with 0 difficulty are excluded to avoid worst-case time complexity of the following sort (e.g. /b/2351871).
        // These notes will not contribute to the difficulty.
        val difficulties = ArrayList<Double>(objectDifficulties.size)

        for (difficulty in objectDifficulties) {
            if (difficulty > 0) {
                difficulties.add(difficulty)
            }
        }

        if (difficulties.isEmpty()) {
            return 0.0
        }

        applyDifficultyTransformation(difficulties)

        var difficulty = 0.0

        noteWeightSum = 0.0

        difficulties.sortDescending()

        for ((index, objectDifficulty) in difficulties.withIndex()) {
            val base = 1 + harmonicScale / (1 + index)
            val weight = base / (index.toDouble().pow(decayExponent) + base)

            noteWeightSum += weight
            difficulty += objectDifficulty * weight
        }

        return difficulty
    }

    /**
     * Calculates the amount of object difficulties weighed against the top object difficulty.
     *
     * @param difficultyValue The final difficulty value.
     */
    fun countTopWeightedObjectDifficulties(difficultyValue: Double): Double {
        if (difficultyValue == 0.0 || noteWeightSum == 0.0) {
            return 0.0
        }

        // This is what the top object difficulty is if all object difficulties were identical.
        val consistentTopNote = difficultyValue / noteWeightSum

        if (consistentTopNote == 0.0) {
            return 0.0
        }

        return objectDifficulties.sumOf {
            DifficultyCalculationUtils.logistic(it / consistentTopNote, 0.88, 10.0, 1.1)
        }
    }

    /**
     * Transforms the difficulties of [TObject]s before they are summed together.
     *
     * This can be used to decrease weight of certain [TObject]s based on a skill-specific criteria.
     *
     * @param difficulties The difficulties of [TObject]s to transform.
     */
    protected open fun applyDifficultyTransformation(difficulties: MutableList<Double>) {}

    override fun processInternal(current: TObject) = objectDifficultyOf(current)

    /**
     * Calculates the difficulty value of a [TObject]. This value is calculated with or without respect to previous
     * [TObject]s.
     *
     * @param current The [TObject] for which the difficulty value should be calculated.
     * @return The difficulty value of [current].
     */
    protected abstract fun objectDifficultyOf(current: TObject): Double

    companion object {
        /**
         * Converts a difficulty value to a performance value.
         *
         * @param difficulty The difficulty value.
         * @return The performance value.
         */
        @JvmStatic
        fun difficultyToPerformance(difficulty: Double) = 4 * difficulty.pow(3)
    }
}