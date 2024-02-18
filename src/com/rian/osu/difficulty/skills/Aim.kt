package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.evaluators.AimEvaluator.evaluateDifficultyOf
import com.rian.osu.mods.Mod
import kotlin.math.pow

/**
 * Represents the skill required to correctly aim at every object in the map with a uniform circle size and normalized distances.
 */
class Aim(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>,

    /**
     * Whether to consider sliders in the calculation.
     */
    private val withSliders: Boolean
) : StrainSkill(mods) {
    private var currentStrain = 0.0
    private val skillMultiplier = 23.55
    private val strainDecayBase = 0.15

    override fun strainValueAt(current: DifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += evaluateDifficultyOf(current, withSliders) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    override fun saveToHitObject(current: DifficultyHitObject) {
        if (withSliders) {
            current.aimStrainWithSliders = currentStrain
        } else {
            current.aimStrainWithoutSliders = currentStrain
        }
    }

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
