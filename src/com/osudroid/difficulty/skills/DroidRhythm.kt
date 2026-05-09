package com.osudroid.difficulty.skills

import com.osudroid.difficulty.DroidDifficultyHitObject
import com.osudroid.difficulty.evaluators.DroidRhythmEvaluator
import com.osudroid.mods.Mod
import com.osudroid.mods.ModScoreV2
import kotlin.math.pow

/**
 * Represents the skill required to properly follow a beatmap's rhythm.
 */
class DroidRhythm(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>
) : DroidStrainSkill(mods) {
    override val reducedSectionCount = 5
    override val starsPerDouble = 1.75

    private var currentStrain = 0.0
    private val strainDecayBase = 0.3

    private val useSliderAccuracy = mods.any { it is ModScoreV2 }

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        val rhythmMultiplier = DroidRhythmEvaluator.evaluateDifficultyOf(current, useSliderAccuracy)
        val doubletapness = 1 - current.getDoubletapness(current.next(0))

        current.rhythmMultiplier = rhythmMultiplier * doubletapness

        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += (rhythmMultiplier - 1) * doubletapness

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}