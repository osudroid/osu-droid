package com.osudroid.difficulty.skills

import com.osudroid.difficulty.StandardDifficultyHitObject
import com.osudroid.difficulty.evaluators.StandardFlashlightEvaluator.evaluateDifficultyOf
import com.osudroid.mods.Mod
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
    private val skillMultiplier = 0.056
    private val strainDecayBase = 0.15

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += evaluateDifficultyOf(current, mods) * skillMultiplier

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
