package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.evaluators.DroidTapEvaluator
import com.rian.osu.mods.Mod
import kotlin.math.exp
import kotlin.math.pow

/**
 * Represents the skill required to press keys or tap with regards to keeping up with the speed at which objects need to be hit.
 */
class DroidTap(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: List<Mod>,

    /**
     * Whether to consider cheesability.
     */
    private val considerCheesability: Boolean,

    /**
     * The strain time to cap to.
     */
    private val strainTimeCap: Double? = null
) : DroidStrainSkill(mods) {
    override val starsPerDouble = 1.1

    private var currentStrain = 0.0
    private var currentRhythm = 0.0

    private val skillMultiplier = 1375.0
    private val strainDecayBase = 0.3

    private val objectDeltaTimes = mutableListOf<Double>()

    /**
     * Gets the amount of notes that are relevant to the difficulty.
     */
    fun relevantNoteCount() = objectStrains.run {
        if (isEmpty()) {
            return 0.0
        }

        val maxStrain = max()
        if (maxStrain == 0.0) {
            return 0.0
        }

        fold(0.0) { acc, d -> acc + 1 / (1 + exp(-(d / maxStrain * 12 - 6))) }
    }

    /**
     * Gets the delta time relevant to the difficulty.
     */
    fun relevantDeltaTime() = objectStrains.run {
        if (isEmpty()) {
            return 0.0
        }

        val maxStrain = max()
        if (maxStrain == 0.0) {
            return 0.0
        }

        objectDeltaTimes.foldIndexed(0.0) { i, acc, d ->
            acc + d / (1 + exp(-(this[i] / maxStrain * 25 - 20)))
        } / fold(0.0) { acc, d ->
            acc + 1 / (1 + exp(-(d / maxStrain * 25 - 20)))
        }
    }

    override fun strainValueAt(current: DroidDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.strainTime)
        currentStrain += DroidTapEvaluator.evaluateDifficultyOf(
            current, considerCheesability, strainTimeCap
        ) * skillMultiplier

        currentRhythm = current.rhythmMultiplier

        val totalStrain = currentStrain * currentRhythm

        objectStrains.add(totalStrain)
        objectDeltaTimes.add(current.deltaTime)

        return totalStrain
    }

    override fun calculateInitialStrain(time: Double, current: DroidDifficultyHitObject) =
        currentStrain * currentRhythm * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}