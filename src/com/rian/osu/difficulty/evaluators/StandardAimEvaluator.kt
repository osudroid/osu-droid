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
 * An evaluator for calculating osu!standard aim skill.
 */
object StandardAimEvaluator {
    private const val WIDE_ANGLE_MULTIPLIER = 1.5
    private const val ACUTE_ANGLE_MULTIPLIER = 2.6
    private const val SLIDER_MULTIPLIER = 1.35
    private const val VELOCITY_CHANGE_MULTIPLIER = 0.75
    private const val WIGGLE_MULTIPLIER = 1.02

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
        if (current.obj is Spinner || current.index <= 1 || current.previous(0)!!.obj is Spinner) {
            return 0.0
        }

        val last = current.previous(0)!!
        val lastLast = current.previous(1)!!

        val radius = DifficultyHitObject.NORMALIZED_RADIUS
        val diameter = DifficultyHitObject.NORMALIZED_DIAMETER

        // Calculate the velocity to the current hit object, which starts with a base distance / time assuming the last object is a circle.
        var currentVelocity = current.lazyJumpDistance / current.strainTime

        // But if the last object is a slider, then we extend the travel velocity through the slider into the current object.
        if (last.obj is Slider && withSliders) {
            // Calculate the slider velocity from slider head to slider end.
            val travelVelocity = last.travelDistance / last.travelTime

            // Calculate the movement velocity from slider end to current object.
            val movementVelocity = current.minimumJumpDistance / current.minimumJumpTime

            // Take the larger total combined velocity.
            currentVelocity = max(currentVelocity, movementVelocity + travelVelocity)
        }

        // As above, do the same for the previous hit object.
        var prevVelocity = last.lazyJumpDistance / last.strainTime
        if (lastLast.obj is Slider && withSliders) {
            val travelVelocity = lastLast.travelDistance / lastLast.travelTime
            val movementVelocity = last.minimumJumpDistance / last.minimumJumpTime

            prevVelocity = max(prevVelocity, movementVelocity + travelVelocity)
        }

        var wideAngleBonus = 0.0
        var acuteAngleBonus = 0.0
        var sliderBonus = 0.0
        var velocityChangeBonus = 0.0
        var wiggleBonus = 0.0

        // Start strain with regular velocity.
        var strain = currentVelocity

        if (
            // If rhythms are the same.
            max(current.strainTime, last.strainTime) < 1.25 * min(current.strainTime, last.strainTime) &&
            current.angle != null && last.angle != null
        ) {
            val currentAngle = current.angle!!
            val lastAngle = last.angle!!

            // Rewarding angles, take the smaller velocity as base.
            val angleBonus = min(currentVelocity, prevVelocity)
            wideAngleBonus = calculateWideAngleBonus(currentAngle)
            acuteAngleBonus = calculateAcuteAngleBonus(currentAngle)

            // Penalize angle repetition.
            wideAngleBonus *= 1 - min(wideAngleBonus, calculateWideAngleBonus(lastAngle).pow(3))
            acuteAngleBonus *= 0.08 + 0.92 * (1 - min(acuteAngleBonus, calculateAcuteAngleBonus(lastAngle).pow(3)))

            // Apply full wide angle bonus for distance more than one diameter.
            wideAngleBonus *= angleBonus * DifficultyCalculationUtils.smootherstep(current.lazyJumpDistance, 0.0, diameter.toDouble())

            // Apply acute angle bonus for BPM above 300 1/2 and distance more than one diameter
            acuteAngleBonus *=
                angleBonus *
                DifficultyCalculationUtils.smootherstep(DifficultyCalculationUtils.millisecondsToBPM(current.strainTime, 2), 300.0, 400.0) *
                DifficultyCalculationUtils.smootherstep(current.lazyJumpDistance, diameter.toDouble(), diameter * 2.0)

            // Apply wiggle bonus for jumps that are [radius, 3*diameter] in distance, with < 110 angle
            // https://www.desmos.com/calculator/dp0v0nvowc
            wiggleBonus = angleBonus *
                DifficultyCalculationUtils.smootherstep(current.lazyJumpDistance, radius.toDouble(), diameter.toDouble()) *
                Interpolation.reverseLinear(current.lazyJumpDistance, diameter * 3.0, diameter.toDouble()).pow(1.8) *
                DifficultyCalculationUtils.smootherstep(currentAngle, 110.0.toRadians(), 60.0.toRadians()) *
                DifficultyCalculationUtils.smootherstep(last.lazyJumpDistance, radius.toDouble(), diameter.toDouble()) *
                Interpolation.reverseLinear(last.lazyJumpDistance, diameter * 3.0, diameter.toDouble()).pow(1.8) *
                DifficultyCalculationUtils.smootherstep(lastAngle, 110.0.toRadians(), 60.0.toRadians())
        }

        if (max(prevVelocity, currentVelocity) != 0.0) {
            // We want to use the average velocity over the whole object when awarding differences, not the individual jump and slider path velocities.
            prevVelocity = (last.lazyJumpDistance + lastLast.travelDistance) / last.strainTime
            currentVelocity = (current.lazyJumpDistance + last.travelDistance) / current.strainTime

            // Scale with ratio of difference compared to half the max distance.
            val distanceRatio =
                sin(Math.PI / 2 * abs(prevVelocity - currentVelocity) / max(prevVelocity, currentVelocity)).pow(2.0)

            // Reward for % distance up to 125 / strainTime for overlaps where velocity is still changing.
            val overlapVelocityBuff =
                min(125 / min(current.strainTime, last.strainTime), abs(prevVelocity - currentVelocity))

            velocityChangeBonus = overlapVelocityBuff * distanceRatio

            // Penalize for rhythm changes.
            velocityChangeBonus *=
                (min(current.strainTime, last.strainTime) / max(current.strainTime, last.strainTime)).pow(2.0)
        }

        if (last.obj is Slider) {
            // Reward sliders based on velocity.
            sliderBonus = last.travelDistance / last.travelTime
        }

        strain += wiggleBonus * WIGGLE_MULTIPLIER

        // Add in acute angle bonus or wide angle bonus + velocity change bonus, whichever is larger.
        strain += max(
            acuteAngleBonus * ACUTE_ANGLE_MULTIPLIER,
            wideAngleBonus * WIDE_ANGLE_MULTIPLIER + velocityChangeBonus * VELOCITY_CHANGE_MULTIPLIER
        )

        // Add in additional slider velocity bonus.
        if (withSliders) {
            strain += sliderBonus * SLIDER_MULTIPLIER
        }

        return strain
    }

    /**
     * Calculates the bonus of wide angles.
     */
    private fun calculateWideAngleBonus(angle: Double) =
        DifficultyCalculationUtils.smoothstep(angle, 40.0.toRadians(), 140.0.toRadians())

    /**
     * Calculates the bonus of acute angles.
     */
    private fun calculateAcuteAngleBonus(angle: Double) =
        DifficultyCalculationUtils.smoothstep(angle, 140.0.toRadians(), 40.0.toRadians())
}
