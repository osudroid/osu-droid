package com.rian.osu.difficulty.skills

import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.evaluators.DroidRhythmEvaluator
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModScoreV2
import kotlin.math.pow

/**
 * Represents the skill required to properly follow a beatmap's rhythm.
 */
class DroidRhythm(
    /**
     * The [Mod]s that this skill processes.
     */
    mods: Iterable<Mod>
) : HarmonicSkill<DroidDifficultyHitObject>(mods) {
    override val harmonicScale = 25.0
    override val decayExponent = 0.8

    private val skillMultiplier = 7.5
    private val difficultyDecayBase = 0.3

    private var currentDifficulty = 0.0
    private val useSliderAccuracy = mods.any { it is ModScoreV2 }

    override fun objectDifficultyOf(current: DroidDifficultyHitObject): Double {
        val rhythmMultiplier = DroidRhythmEvaluator.evaluateDifficultyOf(current, useSliderAccuracy)

        current.rhythmMultiplier = rhythmMultiplier

        currentDifficulty *= strainDecay(current.deltaTime)
        currentDifficulty += (rhythmMultiplier - 1) * skillMultiplier

        return currentDifficulty
    }

    private fun strainDecay(ms: Double) = difficultyDecayBase.pow(ms / 1000)
}