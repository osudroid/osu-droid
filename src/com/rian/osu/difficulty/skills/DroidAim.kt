package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.attributes.DifficultSlider
import com.rian.osu.difficulty.evaluators.DroidAimEvaluator
import com.rian.osu.mods.Mod
import kotlin.math.exp
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
    val withSliders: Boolean
) : DroidStrainSkill(mods) {
    override val starsPerDouble = 1.05

    val sliderVelocities = mutableListOf<DifficultSlider>()
    private val sliderStrains = mutableListOf<Double>()

    private var currentStrain = 0.0
    private val skillMultiplier = 25.6
    private val strainDecayBase = 0.15

    /**
     * Obtains the amount of sliders that are considered difficult in terms of relative strain.
     */
    fun countDifficultSliders(): Double {
        if (sliderStrains.isEmpty()) {
            return 0.0
        }

        val sortedStrains = sliderStrains.sortedDescending()
        val maxStrain = sortedStrains[0]

        if (maxStrain == 0.0) {
            return 0.0
        }

        return sortedStrains.fold(0.0) { total, strain ->
            total + 1 / (1 + exp(-((strain / maxStrain) * 12 - 6)))
        }
    }

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += DroidAimEvaluator.evaluateDifficultyOf(current, withSliders) * skillMultiplier

        val velocity = current.travelDistance / current.travelTime

        if (velocity > 0) {
            sliderVelocities.add(DifficultSlider(current.index + 1, velocity))
        }

        if (current.obj is Slider) {
            sliderStrains.add(currentStrain)
        }

        objectStrains.add(currentStrain)
        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}