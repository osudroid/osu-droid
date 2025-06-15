package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.evaluators.DroidFlashlightEvaluator
import com.rian.osu.mods.Mod
import kotlin.math.pow

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
class DroidFlashlight(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>,

    /**
     * Whether to consider sliders in the calculation.
     */
    @JvmField
    val withSliders: Boolean
) : DroidStrainSkill(mods) {
    override val starsPerDouble = 1.06

    override val reducedSectionCount = 0
    override val reducedSectionBaseline = 1.0

    private var currentStrain = 0.0
    private val skillMultiplier = 0.023
    private val strainDecayBase = 0.15

    override fun difficultyValue() = currentStrainPeaks.sum() * starsPerDouble

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += DroidFlashlightEvaluator.evaluateDifficultyOf(current, mods, withSliders) * skillMultiplier

        objectStrains.add(currentStrain)
        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}