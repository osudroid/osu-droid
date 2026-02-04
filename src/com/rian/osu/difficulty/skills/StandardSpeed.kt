package com.rian.osu.difficulty.skills

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.evaluators.StandardRhythmEvaluator
import com.rian.osu.difficulty.evaluators.StandardSpeedEvaluator
import com.rian.osu.difficulty.utils.StrainUtils
import com.rian.osu.mods.Mod
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/**
 * Represents the skill required to press keys or tap with regards to keeping up with the speed at which objects need to be hit.
 */
class StandardSpeed(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>
) : StandardStrainSkill(mods) {
    override val reducedSectionCount = 5

    private var currentStrain = 0.0
    private var maxStrain = 0.0
    private var currentRhythm = 0.0
    private val skillMultiplier = 1.47
    private val strainDecayBase = 0.3

    private val sliderStrains = mutableListOf<Double>()

    /**
     * Calculates the number of clickable objects weighted by difficulty.
     */
    fun relevantNoteCount(): Double {
        if (objectStrains.isEmpty() || maxStrain == 0.0) {
            return 0.0
        }

        return objectStrains.fold(0.0) { acc, d -> acc + 1 / (1 + exp(-(d / maxStrain * 12 - 6))) }
    }

    fun countTopWeightedSliders() = StrainUtils.countTopWeightedSliders(sliderStrains, difficulty)

    override fun strainValueAt(current: StandardDifficultyHitObject): Double {
        currentStrain *= strainDecay(current.strainTime)
        currentStrain += StandardSpeedEvaluator.evaluateDifficultyOf(current, mods) * skillMultiplier

        currentRhythm = StandardRhythmEvaluator.evaluateDifficultyOf(current)
        val totalStrain = currentStrain * currentRhythm

        maxStrain = max(maxStrain, totalStrain)
        objectStrains.add(totalStrain)

        if (current.obj is Slider) {
            sliderStrains.add(totalStrain)
        }

        return totalStrain
    }

    override fun calculateInitialStrain(time: Double, current: StandardDifficultyHitObject) =
        currentStrain * currentRhythm * strainDecay(time - current.previous(0)!!.startTime)

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
