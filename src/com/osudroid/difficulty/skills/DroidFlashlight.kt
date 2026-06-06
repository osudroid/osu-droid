package com.osudroid.difficulty.skills

import com.osudroid.difficulty.DroidDifficultyHitObject
import com.osudroid.difficulty.evaluators.DroidFlashlightEvaluator
import com.osudroid.mods.Mod
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModRelax
import kotlin.math.min
import kotlin.math.pow

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
class DroidFlashlight(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>,

    private val totalObjects: Int
) : DroidStrainSkill(mods) {
    override val starsPerDouble = 1.06

    override val reducedSectionCount = 0
    override val reducedSectionBaseline = 1.0

    private var currentStrain = 0.0
    private val skillMultiplier = 0.024
    private val strainDecayBase = 0.15

    override fun difficultyValue(): Double {
        var sum = currentStrainPeaks.sum() * starsPerDouble

        // Account for shorter beatmaps having a higher ratio of 0 combo/100 combo flashlight radius.
        sum *= 0.7 + 0.1 * min(1, totalObjects / 200) +
            (if (totalObjects > 200) 0.2 * min(1, (totalObjects - 200) / 200) else 0.0)

        return sum
    }

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += calculateAdjustedDifficulty(current) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun calculateAdjustedDifficulty(current: DroidDifficultyHitObject): Double {
        var difficulty = DroidFlashlightEvaluator.evaluateDifficultyOf(current, mods).pow(0.8)

        if (mods.any { it is ModRelax }) {
            difficulty *= 0.7
        } else if (mods.any { it is ModAutopilot }) {
            difficulty *= 0.4
        }

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