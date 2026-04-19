package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.math.Interpolation
import com.rian.osu.math.toDegrees
import com.rian.osu.math.toRadians
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModHidden
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * An evaluator for calculating osu!droid reading difficulty.
 */
object DroidReadingEvaluator {
    private const val READING_WINDOW_SIZE = 3000.0
    private val DISTANCE_INFLUENCE_THRESHOLD = DifficultyHitObject.NORMALIZED_DIAMETER * 1.25 // 1.25 circles distance between centers
    private const val HIDDEN_MULTIPLIER = 0.28
    private const val DENSITY_MULTIPLIER = 2.4
    private const val DENSITY_DIFFICULTY_BASE = 2.5
    private const val PREEMPT_BALANCING_FACTOR = 150000.0
    private const val PREEMPT_STARTING_POINT = 500 // AR 9.66 in milliseconds
    private const val MINIMUM_ANGLE_RELEVANCY_TIME = 2000.0 // 2 seconds
    private const val MAXIMUM_ANGLE_RELEVANCY_TIME = 200.0

    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, mods: Iterable<Mod>): Double {
        if (current.obj is Spinner || current.isOverlapping(true) || current.index <= 0) {
            return 0.0
        }

        val next = current.next(0) as? DroidDifficultyHitObject

        // Only allow velocity to buff
        val velocity = max(1.0, current.lazyJumpDistance / current.strainTime)

        val currentVisibleObjectDensity = retrieveCurrentVisibleObjectDensity(current)
        val pastObjectDifficultyInfluence = getPastObjectDifficultyInfluence(current)
        val constantAngleNerfFactor = getConstantAngleNerfFactor(current)

        val noteDensityDifficulty = calculateDensityDifficulty(
            next, velocity, constantAngleNerfFactor, pastObjectDifficultyInfluence, currentVisibleObjectDensity
        )

        val hiddenDifficulty = calculateHiddenDifficulty(
            current, mods, pastObjectDifficultyInfluence, currentVisibleObjectDensity, velocity, constantAngleNerfFactor
        )

        val preemptDifficulty = calculatePreemptDifficulty(velocity, constantAngleNerfFactor, current.timePreempt)

        return DifficultyCalculationUtils.norm(1.5, preemptDifficulty, hiddenDifficulty, noteDensityDifficulty)
    }

    /**
     * Calculates the density difficulty of the current object and how hard it is to aim it because of it based on:
     *
     * - cursor velocity to the current object,
     * - how many times the current object's angle was repeated,
     * - density of objects visible when the current object appears, and
     * - density of objects visible when the current object needs to be clicked.
     */
    private fun calculateDensityDifficulty(
        next: DroidDifficultyHitObject?,
        velocity: Double,
        constantAngleNerfFactor: Double,
        pastObjectDifficultyInfluence: Double,
        currentVisibleObjectDensity: Double
    ): Double {
        // Consider future densities too because it can make the path the cursor takes less clear.
        var futureObjectDifficultyInfluence = sqrt(currentVisibleObjectDensity)

        if (next != null) {
            // Reduce difficulty if movement to next object is small.
            futureObjectDifficultyInfluence *=
                DifficultyCalculationUtils.smootherstep(next.lazyJumpDistance, 15.0, DISTANCE_INFLUENCE_THRESHOLD)
        }

        // Value higher note densities exponentially.
        var noteDensityDifficulty =
            (pastObjectDifficultyInfluence + futureObjectDifficultyInfluence).pow(1.7) * 0.4 * constantAngleNerfFactor * velocity

        // Award only denser than average maps.
        noteDensityDifficulty = max(0.0, noteDensityDifficulty - DENSITY_DIFFICULTY_BASE)

        // Apply a soft cap to general density reading to account for partial memorization.
        noteDensityDifficulty = sqrt(noteDensityDifficulty) * DENSITY_MULTIPLIER

        return noteDensityDifficulty
    }

    /**
     * Calculates the difficulty of aiming the current object when the approach rate is very high based on:
     *
     * - cursor velocity to the current object,
     * - how many times the current object's angle was repeated, and
     * - how many milliseconds elapse between the approach circle appearing and touching the inner circle.
     */
    private fun calculatePreemptDifficulty(velocity: Double, constantAngleNerfFactor: Double, preempt: Double): Double {
        // Arbitrary curve for the base value preempt difficulty should have as approach rate increases.
        // https://www.desmos.com/calculator/c175335a71
        var preemptDifficulty =
            ((PREEMPT_STARTING_POINT - preempt + abs(preempt - PREEMPT_STARTING_POINT)) / 2).pow(2.5) / PREEMPT_BALANCING_FACTOR

        preemptDifficulty *= constantAngleNerfFactor * velocity

        return preemptDifficulty
    }

    private fun calculateHiddenDifficulty(
        current: DroidDifficultyHitObject,
        mods: Iterable<Mod>,
        pastObjectDifficultyInfluence: Double,
        currentVisibleObjectDensity: Double,
        velocity: Double,
        constantAngleNerfFactor: Double
    ): Double {
        val hidden = mods.find { it is ModHidden } as? ModHidden

        if (hidden?.onlyFadeApproachCircles != false) {
            return 0.0
        }

        // Higher preempt means that time spent invisible is higher too, we want to reward that.
        val preemptFactor = current.timePreempt.pow(2.2) * 0.01

        // Account for both past and current densities.
        val densityFactor = (currentVisibleObjectDensity + pastObjectDifficultyInfluence).pow(3.3) * 3

        var hiddenDifficulty = (preemptFactor + densityFactor) * constantAngleNerfFactor * velocity * 0.01

        // Apply a soft cap to general Hidden reading to account for partial memorization.
        hiddenDifficulty = hiddenDifficulty.pow(0.4) * HIDDEN_MULTIPLIER

        val prev = current.previous(0)!!

        // Buff perfect stacks only if current note is completely invisible at the time the previous note was clicked.
        if (
            current.lazyJumpDistance == 0.0 &&
            current.opacityAt(prev.obj.startTime, mods) == 0.0 &&
            // At the same time, we only want to buff them if the current note is already
            // animating at the time the previous note was clicked.
            prev.startTime > current.startTime - current.timePreempt
        ) {
            // Perfect stacks are harder the less time between notes.
            hiddenDifficulty += HIDDEN_MULTIPLIER * 2500 / current.strainTime.pow(1.5)
        }

        return hiddenDifficulty
    }

    private fun getPastObjectDifficultyInfluence(current: DroidDifficultyHitObject): Double {
        var pastObjectDifficultyInfluence = 0.0

        for (loopObj in retrievePastVisibleObjects(current)) {
            var loopDifficulty = current.opacityAt(loopObj.obj.startTime)

            // When aiming an object small distances mean previous objects may be cheesed,
            // so it doesn't matter whether they were arranged confusingly.
            loopDifficulty *=
                DifficultyCalculationUtils.smootherstep(loopObj.lazyJumpDistance, 15.0, DISTANCE_INFLUENCE_THRESHOLD)

            // Account less for objects close to the maximum reading window.
            loopDifficulty *= getTimeNerfFactor(current.startTime - loopObj.startTime)

            pastObjectDifficultyInfluence += loopDifficulty
        }

        return pastObjectDifficultyInfluence
    }

    /**
     * Returns the density of objects visible at the point in time the current object needs to be clicked capped by the reading window.
     */
    private fun retrieveCurrentVisibleObjectDensity(current: DroidDifficultyHitObject): Double {
        var visibleObjectCount = 0.0
        var hitObject = current.next(0) as? DroidDifficultyHitObject

        while (hitObject != null) {
            if (
                hitObject.startTime - current.startTime > READING_WINDOW_SIZE ||
                // Object not visible at the time current object needs to be clicked.
                current.startTime < hitObject.startTime - hitObject.timePreempt
            ) {
                break
            }

            if (hitObject.isOverlapping(true)) {
                hitObject = hitObject.next(0) as? DroidDifficultyHitObject
                continue
            }

            val timeNerfFactor = getTimeNerfFactor(hitObject.startTime - current.startTime)
            visibleObjectCount += hitObject.opacityAt(current.obj.startTime) * timeNerfFactor

            hitObject = hitObject.next(0) as? DroidDifficultyHitObject
        }

        return visibleObjectCount
    }

    /**
     * Retrieves a list of objects that are visible at the point in time the current object needs to be hit.
     *
     * @param current The current object.
     */
    private fun retrievePastVisibleObjects(current: DroidDifficultyHitObject) = sequence {
        for (i in 0 until current.index) {
            val hitObject = current.previous(i) as? DroidDifficultyHitObject ?: break

            if (current.startTime - hitObject.startTime > READING_WINDOW_SIZE ||
                // The previous object is not visible at the time the current object needs to be hit.
                hitObject.startTime < current.startTime - current.timePreempt) {
                break
            }

            if (hitObject.isOverlapping(true)) {
                continue
            }

            yield(hitObject)
        }
    }

    /**
     * Returns a factor of how often the current object's angle has been repeated in a certain time frame.
     * It does this by checking the difference in angle between current and past objects and sums them based on a range of similarity.
     * https://www.desmos.com/calculator/eb057a4822
     *
     * @param current The current object.
     */
    private fun getConstantAngleNerfFactor(current: DroidDifficultyHitObject): Double {
        var constantAngleCount = 0.0
        var index = 0
        var currentTimeGap = 0.0

        val currentAngle = current.angle

        var loopObjPrev0 = current
        var loopObjPrev1: DroidDifficultyHitObject? = null
        var loopObjPrev2: DroidDifficultyHitObject? = null

        while (currentTimeGap < MINIMUM_ANGLE_RELEVANCY_TIME) {
            val loopObj = current.previous(index) as? DroidDifficultyHitObject ?: break
            val loopObjAngle = loopObj.angle

            // Account less for objects that are close to the time limit.
            val longIntervalFactor = 1 - Interpolation.reverseLinear(loopObj.strainTime, MAXIMUM_ANGLE_RELEVANCY_TIME, MINIMUM_ANGLE_RELEVANCY_TIME)

            if (loopObjAngle != null && currentAngle != null) {
                val angleDifference = abs(currentAngle - loopObjAngle)
                var angleDifferenceAlternating = PI

                val loopObjPrev0Angle = loopObjPrev0.angle
                val loopObjPrev1Angle = loopObjPrev1?.angle
                val loopObjPrev2Angle = loopObjPrev2?.angle

                if (loopObjPrev0Angle != null && loopObjPrev1Angle != null && loopObjPrev2Angle != null) {
                    angleDifferenceAlternating = abs(loopObjPrev1Angle - loopObjAngle)
                    angleDifferenceAlternating += abs(loopObjPrev2Angle - loopObjPrev0Angle)

                    // Be sure that one of the angles is very sharp, when other is wide.
                    val weight =
                        Interpolation.reverseLinear(min(loopObjAngle, loopObjPrev0Angle).toDegrees(), 20.0, 5.0) *
                        Interpolation.reverseLinear(max(loopObjAngle, loopObjPrev0Angle).toDegrees(), 60.0, 120.0)

                    // Interpolate between max angle difference and rescaled alternating difference, with
                    // harsher scaling compared to normal difference.
                    angleDifferenceAlternating = Interpolation.linear(PI, 0.1 * angleDifferenceAlternating, weight)
                }

                val stackFactor = DifficultyCalculationUtils.smootherstep(
                    loopObj.lazyJumpDistance, 0.0, DifficultyHitObject.NORMALIZED_RADIUS.toDouble()
                )

                constantAngleCount += cos(
                    3 * min(30.0.toRadians(), min(angleDifference, angleDifferenceAlternating) * stackFactor)
                ) * longIntervalFactor
            }

            currentTimeGap = current.startTime - loopObj.startTime
            index++

            loopObjPrev2 = loopObjPrev1
            loopObjPrev1 = loopObjPrev0
            loopObjPrev0 = loopObj
        }

        return (2 / constantAngleCount).coerceIn(0.2, 1.0)
    }

    private fun getTimeNerfFactor(deltaTime: Double) = (2 - deltaTime / (READING_WINDOW_SIZE / 2)).coerceIn(0.0, 1.0)
}