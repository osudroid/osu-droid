package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.math.ErrorFunction
import kotlin.math.max
import kotlin.math.pow

/**
 * An evaluator for calculating osu!droid tap difficulty.
 */
object DroidTapEvaluator {
    // ~200 1/4 BPM streams
    private const val MIN_SPEED_BONUS = 75.0

    /**
     * Evaluates the difficulty of tapping the current object, based on:
     *
     * * time between pressing the previous and current object,
     * * distance between those objects,
     * * and how easily they can be cheesed.
     *
     * @param current The current object.
     * @param considerCheesability Whether to consider cheesability.
     * @param strainTimeCap The strain time to cap to.
     */
    @JvmStatic
    @JvmOverloads
    fun evaluateDifficultyOf(
        current: DroidDifficultyHitObject,
        considerCheesability: Boolean,
        strainTimeCap: Double? = null
    ): Double {
        if (
            current.obj is Spinner ||
            // Exclude overlapping objects that can be tapped at once.
            current.isOverlapping(false)
        ) {
            return 0.0
        }

        val doubletapness = if (considerCheesability) 1 - current.doubletapness else 1.0

        val strainTime =
            if (strainTimeCap != null) max(50.0, max(strainTimeCap, current.strainTime))
            else current.strainTime

        var speedBonus = 1.0

        if (current.strainTime < MIN_SPEED_BONUS) {
            speedBonus += 0.75 * ErrorFunction.erf((MIN_SPEED_BONUS - strainTime) / 40).pow(2)
        }

        return speedBonus * doubletapness.pow(1.5) / strainTime
    }
}