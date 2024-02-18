package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.math.ErrorFunction
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
     * @param greatWindow The great hit window of the current object.
     * @param considerCheesability Whether to consider cheesability.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, greatWindow: Double, considerCheesability: Boolean): Double {
        if (
            current.obj is Spinner ||
            // Exclude overlapping objects that can be tapped at once.
            current.isOverlapping(false)
        ) {
            return 0.0
        }

        var doubletapness = 1.0

        if (considerCheesability) {
            // Nerf doubletappable doubles.
            val next = current.next(0)

            if (next != null) {
                val greatWindowFull = greatWindow * 2
                val currentDeltaTime = max(1.0, current.deltaTime)
                val nextDeltaTime = max(1.0, next.deltaTime)
                val deltaDifference = abs(nextDeltaTime - currentDeltaTime)

                val speedRatio = currentDeltaTime / max(currentDeltaTime, deltaDifference)
                val windowRatio = min(1.0, currentDeltaTime / greatWindowFull).pow(2)
                doubletapness = speedRatio.pow(1 - windowRatio)
            }
        }

        var speedBonus = 1.0

        if (current.strainTime < MIN_SPEED_BONUS) {
            speedBonus += 0.75 * ErrorFunction.erf((MIN_SPEED_BONUS - current.strainTime) / 40).pow(2)
        }

        return (speedBonus * doubletapness.pow(1.5)) / current.strainTime
    }
}