package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.StandardDifficultyHitObject
import kotlin.math.min
import kotlin.math.pow

/**
 * An evaluator for calculating osu!standard agility aim difficulty.
 */
object StandardAgilityEvaluator {
    private val distanceCap = DifficultyHitObject.NORMALIZED_DIAMETER * 1.2

    /**
     * Evaluates the difficulty of fast aiming the current object.
     *
     * @param current The current object.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: StandardDifficultyHitObject): Double {
        if (current.obj is Spinner) {
            return 0.0
        }

        val prev = current.previous(0)

        val travelDistance = prev?.lazyTravelDistance ?: 0.0
        val distance = travelDistance + current.lazyJumpDistance

        val distanceScaled = min(distance, distanceCap) / distanceCap

        var strain = distanceScaled * 1000 / current.strainTime

        strain *= current.smallCircleBonus.pow(1.5)
        strain *= highBpmBonus(current.strainTime)

        return strain
    }

    private fun highBpmBonus(ms: Double) = 1 / (1 - 0.2.pow(ms / 1000))
}