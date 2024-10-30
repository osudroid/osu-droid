package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.attributes.DifficultSlider
import com.rian.osu.difficulty.evaluators.DroidAimEvaluator
import com.rian.osu.mods.Mod
import kotlin.math.pow

/**
 * Represents the skill required to correctly aim at every object in the map with a uniform CircleSize and normalized distances.
 */
class DroidAim(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>,

    /**
     * Whether to consider sliders in the calculation.
     */
    private val withSliders: Boolean
) : DroidStrainSkill(mods) {
    override val starsPerDouble = 1.05

    val sliderVelocities = mutableListOf<DifficultSlider>()

    private var currentStrain = 0.0
    private val skillMultiplier = 24.55
    private val strainDecayBase = 0.15

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += DroidAimEvaluator.evaluateDifficultyOf(current, withSliders) * skillMultiplier

        val velocity = current.travelDistance / current.travelTime

        if (velocity > 0) {
            sliderVelocities.add(DifficultSlider(current.index + 1, velocity))
        }

        objectStrains.add(currentStrain)
        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}