package com.osudroid.difficulty.skills

import com.osudroid.difficulty.StandardDifficultyHitObject
import com.osudroid.difficulty.evaluators.StandardFlashlightEvaluator
import com.osudroid.mods.Mod
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModRelax
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
class StandardFlashlight(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>,

    private val totalObjects: Int
) : StandardStrainSkill(mods) {
    override val reducedSectionCount = 0
    override val reducedSectionBaseline = 1.0
    override val decayWeight = 1.0

    private var currentStrain = 0.0
    private val skillMultiplier = 0.058
    private val strainDecayBase = 0.15

    private val hasFlashlight = mods.any { it is ModFlashlight }

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        if (!hasFlashlight) {
            return 0.0
        }

        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += calculateAdjustedDifficulty(current) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: StandardDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    override fun difficultyValue(): Double {
        var sum = currentStrainPeaks.sum()

        // Account for shorter beatmaps having a higher ratio of 0 combo/100 combo flashlight radius.
        sum *= 0.7 + 0.1 * min(1, totalObjects / 200) +
            (if (totalObjects > 200) 0.2 * min(1, (totalObjects - 200) / 200) else 0.0)

        return sum
    }

    private fun calculateAdjustedDifficulty(current: StandardDifficultyHitObject): Double {
        var difficulty = StandardFlashlightEvaluator.evaluateDifficultyOf(current, mods)

        if (mods.any { it is ModRelax }) {
            difficulty *= 0.7
        } else if (mods.any { it is ModAutopilot }) {
            difficulty *= 0.4
        }

        difficulty *= 0.985 + max(0.0, current.overallDifficulty).pow(2) / 4000

        return difficulty
    }

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)

    companion object {
        /**
         * Converts a difficulty value to a performance value.
         *
         * @param difficulty The difficulty value.
         * @return The performance value.
         */
        @JvmStatic
        fun difficultyToPerformance(difficulty: Double) = difficulty.pow(2) * 25
    }
}
