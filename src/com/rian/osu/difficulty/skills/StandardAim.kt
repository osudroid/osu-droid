package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.evaluators.StandardAimEvaluator.evaluateDifficultyOf
import com.rian.osu.mods.Mod
import kotlin.math.exp
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
    @JvmField
    val withSliders: Boolean
) : StandardStrainSkill(mods) {
    private var currentStrain = 0.0
    private val skillMultiplier = 25.6
    private val strainDecayBase = 0.15

    private val sliderStrains = mutableListOf<Double>()

    /**
     * Obtains the amount of sliders that are considered difficult in terms of relative strain.
     */
    fun countDifficultSliders(): Double {
        if (sliderStrains.isEmpty()) {
            return 0.0
        }

        val maxStrain = sliderStrains.max()

        if (maxStrain == 0.0) {
            return 0.0
        }

        return sliderStrains.fold(0.0) { total, strain ->
            total + 1 / (1 + exp(-((strain / maxStrain) * 12 - 6)))
        }
    }

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += evaluateDifficultyOf(current, withSliders) * skillMultiplier

        if (current.obj is Slider) {
            sliderStrains.add(currentStrain)
        }

        objectStrains.add(currentStrain)
        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: StandardDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
