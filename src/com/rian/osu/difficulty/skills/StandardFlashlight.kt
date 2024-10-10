package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.evaluators.StandardFlashlightEvaluator.evaluateDifficultyOf
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModHidden
import kotlin.math.pow

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
class StandardFlashlight(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>
) : StandardStrainSkill(mods) {
    override val reducedSectionCount = 0
    override val reducedSectionBaseline = 1.0
    override val decayWeight = 1.0

    private var currentStrain = 0.0
    private val skillMultiplier = 0.05512
    private val strainDecayBase = 0.15
    private val hasHidden = mods.any { it is ModHidden }

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += evaluateDifficultyOf(current, hasHidden) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: StandardDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    override fun difficultyValue() = currentStrainPeaks.sum()

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
