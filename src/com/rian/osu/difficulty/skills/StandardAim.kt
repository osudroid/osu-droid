package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.evaluators.StandardAimEvaluator.evaluateDifficultyOf
import com.rian.osu.mods.Mod
import kotlin.math.pow

/**
 * Represents the skill required to correctly aim at every object in the map with a uniform circle size and normalized distances.
 */
class StandardAim(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>,

    /**
     * Whether to consider sliders in the calculation.
     */
    private val withSliders: Boolean
) : StandardStrainSkill(mods) {
    private var currentStrain = 0.0
    private val skillMultiplier = 25.18
    private val strainDecayBase = 0.15

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += evaluateDifficultyOf(current, withSliders) * skillMultiplier

        objectStrains.add(currentStrain)
        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: StandardDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
