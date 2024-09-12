package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.StandardDifficultyHitObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * An evaluator for calculating osu!standard speed difficulty.
 */
object StandardSpeedEvaluator {
    private const val SINGLE_SPACING_THRESHOLD = 125.0
    private const val MIN_SPEED_BONUS = 75.0

    /**
     * Evaluates the difficulty of tapping the current object, based on:
     *
     *  * time between pressing the previous and current object,
     *  * distance between those objects,
     *  * and how easily they can be cheesed.
     *
     * @param current The current object.
     */
    fun evaluateDifficultyOf(current: StandardDifficultyHitObject): Double {
        if (current.obj is Spinner) {
            return 0.0
        }

        val prev = current.previous(0)
        var strainTime = current.strainTime

        // Nerf double-tappable doubles.
        val next = current.next(0)
        var doubletapness = 1.0

        if (next != null) {
            val currentDeltaTime = max(1.0, current.deltaTime)
            val nextDeltaTime = max(1.0, next.deltaTime)
            val deltaDifference = abs(nextDeltaTime - currentDeltaTime)

            val speedRatio = currentDeltaTime / max(currentDeltaTime, deltaDifference)
            val windowRatio = min(1.0, currentDeltaTime / current.fullGreatWindow).pow(2.0)

            doubletapness = speedRatio.pow(1 - windowRatio)
        }

        // Cap deltatime to the OD 300 hitwindow.
        // 0.93 is derived from making sure 260 BPM 1/4 OD8 streams aren't nerfed harshly, whilst 0.92 limits the effect of the cap.
        strainTime /= (strainTime / current.fullGreatWindow / 0.93).coerceIn(0.92, 1.0)

        var speedBonus = 1.0
        if (strainTime < MIN_SPEED_BONUS) {
            speedBonus += 0.75 * ((MIN_SPEED_BONUS - strainTime) / 40).pow(2.0)
        }

        val travelDistance = prev?.travelDistance ?: 0.0
        val distance = min(SINGLE_SPACING_THRESHOLD, travelDistance + current.minimumJumpDistance)

        return (speedBonus + speedBonus * (distance / SINGLE_SPACING_THRESHOLD).pow(3.5)) * doubletapness / strainTime
    }
}
