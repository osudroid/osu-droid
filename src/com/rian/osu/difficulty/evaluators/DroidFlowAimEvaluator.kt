package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * An evaluator for calculating osu!droid flow aim difficulty.
 */
object DroidFlowAimEvaluator {
    private const val VELOCITY_CHANGE_MULTIPLIER = 0.52

    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, withSliders: Boolean): Double {
        if (current.obj is Spinner || current.index <= 1 || current.previous(0)?.obj is Spinner) {
            return 0.0
        }

        val last = current.previous(0)!!
        val lastLast = current.previous(1)!!

        val currentDistance = if (withSliders) current.lazyJumpDistance else current.jumpDistance
        val prevDistance = if (withSliders) last.lazyJumpDistance else last.jumpDistance

        var currentVelocity = currentDistance / current.strainTime

        if (last.obj is Slider && withSliders) {
            // If the last object is a slider, then we extend the travel velocity through the slider into the current object.
            val sliderDistance = last.lazyTravelDistance + current.lazyJumpDistance

            currentVelocity = max(currentVelocity, sliderDistance / current.strainTime)
        }

        val prevVelocity = prevDistance / last.strainTime
        var difficulty = currentVelocity

        // Apply high circle size bonus to the base velocity.
        // We use reduced CS bonus here because the bonus was made for an evaluator with a different d/t scaling.
        difficulty *= sqrt(current.smallCircleBonus)

        // Rhythm changes are harder to flow.
        difficulty *= 1 + min(
            0.25,
            ((max(current.strainTime, last.strainTime) - min(current.strainTime, last.strainTime)) / 50).pow(4)
        )

        val currentAngle = current.angle
        val lastAngle = last.angle

        if (currentAngle != null && lastAngle != null) {
            // Low angular velocity (consistent angles) is easier to follow than erratic flow.
            val angleDifference = abs(currentAngle - lastAngle)
            val angleDifferenceAdjusted = sin(angleDifference / 2) * 180

            val angularVelocity = angleDifferenceAdjusted / (current.strainTime * 0.1)

            difficulty *= 0.8 + sqrt(angularVelocity / 270)
        }

        // If all three notes overlap, do not reward bonuses as there is no required additional movement.
        var overlappedNotesWeight = 1.0

        if (current.index > 2) {
            val o1 = calculateOverlapFactor(current.obj, last.obj)
            val o2 = calculateOverlapFactor(current.obj, lastLast.obj)
            val o3 = calculateOverlapFactor(last.obj, lastLast.obj)

            overlappedNotesWeight = 1 - o1 * o2 * o3
        }

        if (currentAngle != null) {
            // Acute angles are harder to flow.
            // Square root velocity to ensure acute angle switches in streams are not assessed as harder than snap.
            difficulty += currentVelocity * DroidSnapAimEvaluator.calculateAcuteAngleAcuteness(currentAngle) * overlappedNotesWeight
        }

        if (max(currentVelocity, prevVelocity) > 0) {
            if (withSliders) {
                currentVelocity = currentDistance / current.strainTime
            }

            // Scale with ratio of difference compared to 0.5 * max distance.
            val distanceRatio = DifficultyCalculationUtils.smoothstep(
                abs(prevVelocity - currentVelocity) / max(prevVelocity, currentVelocity),
                0.0,
                1.0
            )

            // Reward for % distance up to 125 / strainTime for overlaps where velocity is still changing.
            val overlapVelocityBuff = min(
                DifficultyHitObject.NORMALIZED_DIAMETER * 1.25 / min(current.strainTime, last.strainTime),
                abs(prevVelocity - currentVelocity)
            )

            difficulty += overlapVelocityBuff * distanceRatio * overlappedNotesWeight * VELOCITY_CHANGE_MULTIPLIER
        }

        if (current.obj is Slider && withSliders) {
            // Include slider velocity to make velocity more consistent with snap.
            difficulty += current.travelDistance / current.travelTime
        }

        // The final velocity is being raised to a power because flow difficulty scales harder with both high
        // distance and time, and we want to account for that.
        difficulty = difficulty.pow(1.45)

        // Reduce difficulty for low spacing since spacing below radius is always to be flowed.
        return difficulty * DifficultyCalculationUtils.smootherstep(currentDistance, 0.0, DifficultyHitObject.NORMALIZED_RADIUS.toDouble())
    }

    private fun calculateOverlapFactor(o1: HitObject, o2: HitObject): Double {
        val distance = o1.difficultyStackedPosition.getDistance(o2.difficultyStackedPosition)
        val radius = o1.difficultyRadius

        return (1 - (max(0.0, distance - radius) / radius).pow(2)).coerceIn(0.0, 1.0)
    }
}