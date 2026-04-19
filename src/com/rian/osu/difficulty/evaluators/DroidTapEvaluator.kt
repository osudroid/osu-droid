package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.math.ErrorFunction
import kotlin.math.pow

/**
 * An evaluator for calculating osu!droid tap difficulty.
 */
object DroidTapEvaluator {
    private const val MIN_SPEED_BONUS = 200.0

    /**
     * Evaluates the difficulty of tapping the current object, based on:
     *
     * * time between pressing the previous and current object,
     * * and how easily they can be cheesed.
     *
     * @param current The current object.
     * @param considerCheesability Whether to consider cheesability.
     */
    @JvmStatic
    fun evaluateDifficultyOf(
        current: DroidDifficultyHitObject,
        considerCheesability: Boolean
    ): Double {
        if (
            current.index < 0 ||
            current.obj is Spinner ||
            // Exclude overlapping objects that can be tapped at once.
            current.isOverlapping(false)
        ) {
            return 0.0
        }

        val doubletapness = if (considerCheesability) 1 - current.getDoubletapness(current.next(0)) else 1.0

        var speedBonus = 1.0

        if (DifficultyCalculationUtils.millisecondsToBPM(current.strainTime) < MIN_SPEED_BONUS) {
            speedBonus += 0.75 * ErrorFunction.erfFast((DifficultyCalculationUtils.bpmToMilliseconds(MIN_SPEED_BONUS) - current.strainTime) / 40).pow(2)
        }

        var strain = speedBonus * 1000 / current.strainTime
        strain *= highBpmBonus(current.strainTime)

        return strain * doubletapness.pow(1.5)
    }

    private fun highBpmBonus(ms: Double) = 1 / (1 - 0.3.pow(ms / 1000))
}