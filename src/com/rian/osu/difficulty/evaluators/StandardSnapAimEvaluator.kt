package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.math.Interpolation
import com.rian.osu.math.toRadians
import kotlin.math.*

/**
 * An evaluator for calculating osu!standard snap aim skill.
 */
object StandardSnapAimEvaluator {
    private const val WIDE_ANGLE_MULTIPLIER = 9.67
    private const val ACUTE_ANGLE_MULTIPLIER = 2.41
    private const val SLIDER_MULTIPLIER = 1.5
    private const val VELOCITY_CHANGE_MULTIPLIER = 0.9

    // Increasing this multiplier beyond 1.02 reduces difficulty as distance increases.
    // Refer to the Desmos link above the wiggle bonus calculation.
    private const val WIGGLE_MULTIPLIER = 1.02

    private const val ANGLE_REPETITION_NOTE_LIMIT = 6
    private const val MAXIMUM_REPETITION_NERF = 0.15
    private const val MAXIMUM_VECTOR_INFLUENCE = 0.5

    /**
     * Evaluates the difficulty of aiming the current object, based on:
     *
     *  * cursor velocity to the current object,
     *  * angle difficulty,
     *  * sharp velocity increases,
     *  * and slider difficulty.
     *
     * @param current The current object.
     * @param withSliders Whether to take slider difficulty into account.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: StandardDifficultyHitObject, withSliders: Boolean): Double {
        val last = current.previous(0)

        if (current.obj is Spinner || current.index <= 1 || last == null || last.obj is Spinner) {
            return 0.0
        }

        val last2 = current.previous(2)

        val radius = current.normalizedRadius
        val diameter = current.normalizedDiameter

        // Calculate the velocity to the current hit object, which starts with a base distance / time assuming the last object is a circle.
        val currentDistance = if (withSliders) current.lazyJumpDistance else current.jumpDistance
        var currentVelocity = currentDistance / current.strainTime

        // But if the last object is a slider, then we extend the travel velocity through the slider into the current object.
        if (last.obj is Slider && withSliders) {
            val sliderDistance = last.lazyTravelDistance + current.lazyJumpDistance

            currentVelocity = max(currentVelocity, sliderDistance / current.strainTime)
        }

        // As above, do the same for the previous hit object.
        val prevDistance = if (withSliders) last.lazyJumpDistance else last.jumpDistance
        val prevVelocity = prevDistance / last.strainTime

        // Start strain with regular velocity.
        var strain = currentVelocity

        strain *= calculateVectorAngleRepetition(current, last)

        val currentAngle = current.angle
        val lastAngle = last.angle

        if (currentAngle != null && lastAngle != null) {
            // Rewarding angles, take the smaller velocity as base.
            val velocityInfluence = min(currentVelocity, prevVelocity)

            var acuteAngleBonus = 0.0

            if (
                // If rhythms are the same.
                max(current.strainTime, last.strainTime) <
                1.25 * min(current.strainTime, last.strainTime)
            ) {
                acuteAngleBonus = calculateAcuteAngleAcuteness(currentAngle)

                // Penalize angle repetition. It is important to do it _before_ multiplying by anything because we compare raw acuteness here.
                acuteAngleBonus *= 0.08 + 0.92 * (1 - min(acuteAngleBonus, calculateAcuteAngleAcuteness(lastAngle).pow(3)))

                // Apply acute angle bonus for BPM above 300 1/2.
                acuteAngleBonus *=
                    velocityInfluence *
                    DifficultyCalculationUtils.smootherstep(DifficultyCalculationUtils.millisecondsToBPM(current.strainTime, 2), 300.0, 400.0) *
                    DifficultyCalculationUtils.smootherstep(currentDistance, 0.0, diameter * 2.0)
            }

            var wideAngleBonus = calculateWideAngleAcuteness(currentAngle)

            // Penalize angle repetition. It is important to do it _before_ multiplying by velocity because we compare raw wideness here.
            wideAngleBonus *= 0.25 + 0.75 * (1 - min(wideAngleBonus, calculateWideAngleAcuteness(lastAngle).pow(3)))

            // Rescale velocity for the wide angle bonus.
            val wideAngleTimeScale = 1.45

            var wideAngleCurrentVelocity = currentDistance / current.strainTime.pow(wideAngleTimeScale)
            val wideAnglePrevVelocity = prevDistance / last.strainTime.pow(wideAngleTimeScale)

            if (last.obj is Slider && withSliders) {
                val sliderDistance = last.lazyTravelDistance + current.lazyJumpDistance

                wideAngleCurrentVelocity = max(
                    wideAngleCurrentVelocity,
                    sliderDistance / current.strainTime.pow(wideAngleTimeScale)
                )
            }

            wideAngleBonus *= min(wideAngleCurrentVelocity, wideAnglePrevVelocity)

            if (last2 != null) {
                // If objects just go back and forth through a middle point - don't give as much wide bonus.
                // Use previous(2) and previous(0) because angles calculation is done prevprev-prev-curr, so any
                // object's angle's center point is always the previous object.
                val distanceSquared = last2.obj.difficultyStackedPosition.getDistanceSquared(last.obj.difficultyStackedPosition)

                if (distanceSquared < 1f) {
                    wideAngleBonus *= 1 - 0.55 * (1 - sqrt(distanceSquared.toDouble()))
                }
            }

            // Add in acute angle bonus or wide angle bonus, whichever is larger.
            strain += max(acuteAngleBonus * ACUTE_ANGLE_MULTIPLIER, wideAngleBonus * WIDE_ANGLE_MULTIPLIER)

            // Apply wiggle bonus for jumps that are [radius, 3*diameter] in distance, with < 110 angle
            // https://www.desmos.com/calculator/dp0v0nvowc
            strain += velocityInfluence *
                DifficultyCalculationUtils.smootherstep(currentDistance, radius.toDouble(), diameter.toDouble()) *
                Interpolation.reverseLinear(currentDistance, diameter * 3.0, diameter.toDouble()).pow(1.8) *
                DifficultyCalculationUtils.smootherstep(currentAngle, 110.0.toRadians(), 60.0.toRadians()) *
                DifficultyCalculationUtils.smootherstep(prevDistance, radius.toDouble(), diameter.toDouble()) *
                Interpolation.reverseLinear(prevDistance, diameter * 3.0, diameter.toDouble()).pow(1.8) *
                DifficultyCalculationUtils.smootherstep(lastAngle, 110.0.toRadians(), 60.0.toRadians()) *
                WIGGLE_MULTIPLIER
        }

        if (max(prevVelocity, currentVelocity) != 0.0) {
            if (withSliders) {
                // We want to use the average velocity over the whole object when awarding differences, not the individual jump and slider path velocities.
                currentVelocity = currentDistance / current.strainTime
            }

            // Scale with ratio of difference compared to half the max distance.
            val distanceRatio = DifficultyCalculationUtils.smoothstep(
                abs(prevVelocity - currentVelocity) / max(prevVelocity, currentVelocity),
                0.0,
                1.0
            )

            // Reward for % distance up to 125 / strainTime for overlaps where velocity is still changing.
            val overlapVelocityBuff =
                min(diameter * 1.25 / min(current.strainTime, last.strainTime), abs(prevVelocity - currentVelocity))

            var velocityChangeBonus = overlapVelocityBuff * distanceRatio

            // Penalize for rhythm changes.
            velocityChangeBonus *=
                (min(current.strainTime, last.strainTime) / max(current.strainTime, last.strainTime)).pow(2)

            strain += velocityChangeBonus * VELOCITY_CHANGE_MULTIPLIER
        }

        if (current.obj is Slider && withSliders) {
            // Reward sliders based on velocity.
            val sliderBonus = current.travelDistance / current.travelTime

            strain +=
                (if (sliderBonus < 1) sliderBonus else sliderBonus.pow(0.75)) * SLIDER_MULTIPLIER
        }

        // Apply high circle size bonus.
        strain *= current.smallCircleBonus

        strain *= highBpmBonus(current.strainTime)

        return strain
    }

    /**
     * Calculates the bonus of wide angles.
     */
    private fun calculateWideAngleAcuteness(angle: Double) =
        DifficultyCalculationUtils.smoothstep(angle, 40.0.toRadians(), 140.0.toRadians())

    /**
     * Calculates the bonus of acute angles.
     */
    @JvmStatic
    fun calculateAcuteAngleAcuteness(angle: Double) =
        DifficultyCalculationUtils.smoothstep(angle, 140.0.toRadians(), 40.0.toRadians())

    private fun highBpmBonus(ms: Double) = 1 / (1 - 0.03.pow((ms / 1000).pow(0.65)))

    private fun calculateVectorAngleRepetition(current: DifficultyHitObject, prev: DifficultyHitObject): Double {
        val currentAngle = current.angle
        val prevAngle = prev.angle

        if (currentAngle == null || prevAngle == null) {
            return 1.0
        }

        val normalizedVectorAngle = current.normalizedVectorAngle
        var constantAngleCount = 0.0

        for (i in 0 until ANGLE_REPETITION_NOTE_LIMIT) {
            val loopObj = current.previous(i) ?: break

            // Only consider vectors in the same jump section, as stopping to change rhythm ruins momentum.
            if (max(current.strainTime, loopObj.strainTime) > 1.1 * min(current.strainTime, loopObj.strainTime)) {
                break
            }

            val loopNormalizedVectorAngle = loopObj.normalizedVectorAngle

            if (normalizedVectorAngle != null && loopNormalizedVectorAngle != null) {
                val angleDifference = abs(normalizedVectorAngle - loopNormalizedVectorAngle)

                // Refer to this Desmos for tuning.
                // Constants need to be precise so that values stay within the range of 0 and 1.
                // https://www.desmos.com/calculator/a8jesv5sv2
                constantAngleCount += cos(8 * min(11.25.toRadians(), angleDifference))
            }
        }

        val vectorRepetition = min(0.5 / constantAngleCount, 1.0).pow(2)

        val stackFactor = DifficultyCalculationUtils.smootherstep(
            current.lazyJumpDistance,
            0.0,
            current.normalizedDiameter.toDouble()
        )

        val angleDifferenceAdjusted = cos(2 * min(45.0.toRadians(), abs(currentAngle - prevAngle) * stackFactor))

        val baseNerf = 1 - MAXIMUM_REPETITION_NERF * calculateAcuteAngleAcuteness(prevAngle) * angleDifferenceAdjusted

        return (baseNerf + (1 - baseNerf) * vectorRepetition * MAXIMUM_VECTOR_INFLUENCE * stackFactor).pow(2)
    }
}
