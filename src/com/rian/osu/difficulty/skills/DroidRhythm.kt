package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.evaluators.DroidRhythmEvaluator
import com.rian.osu.mods.Mod
import kotlin.math.pow

/**
 * Represents the skill required to properly follow a beatmap's rhythm.
 */
class DroidRhythm(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>,

    /**
     * The 300 hit window.
     */
    private val greatWindow: Double
) : DroidStrainSkill(mods) {
    override val reducedSectionCount = 5
    override val starsPerDouble = 1.75

    private var currentStrain = 0.0
    private val strainDecayBase = 0.3

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += DroidRhythmEvaluator.evaluateDifficultyOf(current, greatWindow) - 1

        objectStrains.add(currentStrain)

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}