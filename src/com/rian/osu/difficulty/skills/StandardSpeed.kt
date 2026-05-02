package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.evaluators.StandardRhythmEvaluator
import com.rian.osu.difficulty.evaluators.StandardSpeedEvaluator
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.mods.Mod
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * Represents the skill required to press keys or tap with regards to keeping up with the speed at which objects need to be hit.
 */
class StandardSpeed(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>
) : HarmonicSkill<StandardDifficultyHitObject>(mods) {
    override val harmonicScale = 20.0

    private var currentDifficulty = 0.0
    private var maxDifficulty = 0.0
    private val skillMultiplier = 1.16
    private val strainDecayBase = 0.3

    private val sliderDifficulties = mutableListOf<Double>()

    /**
     * Calculates the number of clickable objects weighted by difficulty.
     */
    fun relevantNoteCount(): Double {
        if (objectDifficulties.isEmpty() || maxDifficulty == 0.0) {
            return 0.0
        }

        return objectDifficulties.sumOf { 1 / (1 + exp(-(it / maxDifficulty * 12 - 6))) }
    }

    /**
     * Obtains the amount of sliders that are considered difficult in terms of relative difficulty, weighted by consistency.
     *
     * @param difficultyValue The final difficulty value.
     */
    fun countTopWeightedSliders(difficultyValue: Double): Double {
        if (sliderDifficulties.isEmpty() || noteWeightSum == 0.0) {
            return 0.0
        }

        // What would the top note be if all note values were identical
        val consistentTopNote = difficultyValue / noteWeightSum

        if (consistentTopNote == 0.0) {
            return 0.0
        }

        // Use a weighted sum of all notes. Constants are arbitrary and give nice values
        return sliderDifficulties.sumOf {
            DifficultyCalculationUtils.logistic(it / consistentTopNote, 0.88, 10.0, 1.1)
        }
    }

    override fun objectDifficultyOf(current: StandardDifficultyHitObject): Double {
        val decay = strainDecay(current.strainTime)

        currentDifficulty *= decay
        currentDifficulty += StandardSpeedEvaluator.evaluateDifficultyOf(current) * (1 - decay) * skillMultiplier

        val currentRhythm = StandardRhythmEvaluator.evaluateDifficultyOf(current)

        val difficulty = currentDifficulty * currentRhythm
        maxDifficulty = max(maxDifficulty, difficulty)

        if (current.obj is Slider) {
            sliderDifficulties.add(difficulty)
        }

        return difficulty
    }

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
