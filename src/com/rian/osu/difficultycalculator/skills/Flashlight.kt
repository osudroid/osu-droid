package com.rian.osu.difficultycalculator.skills

import com.rian.osu.difficultycalculator.DifficultyHitObject
import com.rian.osu.difficultycalculator.evaluators.FlashlightEvaluator.evaluateDifficultyOf
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.*
import kotlin.math.pow

/**
 * Represents the skill required to memorize and hit every object in a beatmap with the Flashlight mod enabled.
 */
class Flashlight(mods: EnumSet<GameMod>) : StrainSkill(mods) {
    override val reducedSectionCount = 0
    override val reducedSectionBaseline = 1.0
    override val decayWeight = 1.0

    private var currentStrain = 0.0
    private val skillMultiplier = 0.052
    private val strainDecayBase = 0.15
    private val hasHidden = GameMod.MOD_HIDDEN in mods

    override fun strainValueAt(current: DifficultyHitObject): Double {
        currentStrain *= strainDecay(current.deltaTime)
        currentStrain += evaluateDifficultyOf(current, hasHidden) * skillMultiplier

        return currentStrain
    }

    override fun calculateInitialStrain(time: Double, current: DifficultyHitObject) =
        currentStrain * strainDecay(time - current.previous(0)!!.startTime)

    override fun saveToHitObject(current: DifficultyHitObject) {
        current.flashlightStrain = currentStrain
    }

    private fun strainDecay(ms: Double) = strainDecayBase.pow(ms / 1000)
}
