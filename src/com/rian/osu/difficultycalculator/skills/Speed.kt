package com.rian.osu.difficultycalculator.skills

import com.rian.osu.difficultycalculator.DifficultyHitObject
import com.rian.osu.difficultycalculator.evaluators.RhythmEvaluator
import com.rian.osu.difficultycalculator.evaluators.SpeedEvaluator
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.*
import kotlin.math.exp
import kotlin.math.pow

/**
 * Represents the skill required to press keys or tap with regards to keeping up with the speed at which objects need to be hit.
 */
class Speed(
    /**
     * The mods that this skill processes.
     */
    mods: EnumSet<GameMod>,

    /**
     * The 300 hit window.
     */
    private val greatWindow: Double
) : StrainSkill(mods) {
    override val difficultyMultiplier = 1.04
    override val reducedSectionCount = 5

    private var currentStrain = 0.0
    private var currentRhythm = 0.0
    private val skillMultiplier = 1375.0
    private val strainDecayBase = 0.3

    private val objectStrains = mutableListOf<Double>()

    /**
     * Calculates the number of clickable objects weighted by difficulty.
     */
    fun relevantNoteCount(): Double = objectStrains.run {
        if (isEmpty()) {
            return 0.0
        }

        val maxStrain = max()
        if (maxStrain == 0.0) {
            return 0.0
        }

        return reduce { acc, d -> acc + 1 / (1 + exp(-(d / maxStrain * 12 - 6))) }
    }

    override fun strainValueAt(current: DifficultyHitObject): Double {
        currentStrain *= strainDecay(current.strainTime)
        currentStrain += SpeedEvaluator.evaluateDifficultyOf(current, greatWindow) * skillMultiplier

        currentRhythm = RhythmEvaluator.evaluateDifficultyOf(current, greatWindow)
        val totalStrain = currentStrain * currentRhythm

        objectStrains.add(totalStrain)
        return totalStrain
    }

    override fun calculateInitialStrain(time: Double, current: DifficultyHitObject) =
        currentStrain * currentRhythm * strainDecay(time - current.previous(0)!!.startTime)

    override fun saveToHitObject(current: DifficultyHitObject) {
        current.speedStrain = currentStrain
        current.rhythmMultiplier = currentRhythm
    }

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
