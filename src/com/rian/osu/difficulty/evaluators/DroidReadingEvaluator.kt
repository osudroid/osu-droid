package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModHidden
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * An evaluator for calculating osu!droid reading difficulty.
 */
object DroidReadingEvaluator {
    private val EMPTY_MODS = emptyList<Mod>()
    private const val READING_WINDOW_SIZE = 3000.0
    private val DISTANCE_INFLUENCE_THRESHOLD = DifficultyHitObject.NORMALIZED_DIAMETER * 1.25 // 1.25 circles distance between centers
    private const val HIDDEN_MULTIPLIER = 0.5
    private const val DENSITY_MULTIPLIER = 0.8
    private const val DENSITY_DIFFICULTY_BASE = 1.5
    private const val PREEMPT_BALANCING_FACTOR = 220000.0
    private const val PREEMPT_STARTING_POINT = 475 // AR 9.83 in milliseconds

    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, clockRate: Double, mods: Iterable<Mod>): Double {
        if (current.obj is Spinner || current.isOverlapping(true) || current.index <= 0) {
            return 0.0
        }

        val constantAngleNerfFactor = getConstantAngleNerfFactor(current)
        // Only allow velocity to buff.
        val velocityFactor = max(1.0, current.minimumJumpDistance / current.strainTime)

        var pastObjectDifficultyInfluence = 0.0

        for (prev in retrievePastVisibleObjects(current)) {
            var prevDifficulty = current.opacityAt(prev.obj.startTime, EMPTY_MODS)

            // Small distances mean objects may be cheesed, so it does not matter whether they are arranged confusingly.
            prevDifficulty *= DifficultyCalculationUtils.smootherstep(prev.lazyJumpDistance, 15.0, DISTANCE_INFLUENCE_THRESHOLD)

            // Account less for objects close to the maximum reading window.
            prevDifficulty *= getTimeNerfFactor(current.startTime - prev.startTime)

            pastObjectDifficultyInfluence += prevDifficulty
        }

        // Value higher note densities exponentially.
        var noteDensityDifficulty = pastObjectDifficultyInfluence.pow(1.45) * 0.9 * constantAngleNerfFactor * velocityFactor

        // Award only denser than average beatmaps.
        noteDensityDifficulty = max(0.0, noteDensityDifficulty - DENSITY_DIFFICULTY_BASE)

        // Apply a soft cap to general density reading to account for partial memorization.
        noteDensityDifficulty = noteDensityDifficulty.pow(0.8) * DENSITY_MULTIPLIER

        var hiddenDifficulty = 0.0

        if (mods.any { it is ModHidden }) {
            val timeSpentInvisible = getDurationSpentInvisible(current) / clockRate

            // Value time spent invisible exponentially.
            val timeSpentInvisibleFactor = timeSpentInvisible.pow(2.1) * 0.0001

            // Buff current object if upcoming objects are dense. This is on the basis that part of
            // Hidden difficulty is the uncertainty of the current cursor position in relation to
            // future notes.
            val futureObjectDifficultyInfluence = calculateCurrentVisibleObjectsDensity(current)

            // Account for both past and current densities.
            val densityFactor = max(1.0, futureObjectDifficultyInfluence + pastObjectDifficultyInfluence - 2).pow(2.3) * 3.2

            hiddenDifficulty += (timeSpentInvisibleFactor + densityFactor) *  constantAngleNerfFactor * velocityFactor * 0.007

            // Apply a soft cap to general Hidden reading to account for partial memorization.
            hiddenDifficulty = hiddenDifficulty.pow(0.65) * HIDDEN_MULTIPLIER

            val prev = current.previous(0) as DroidDifficultyHitObject

            // Buff perfect stacks only if the current object is completely invisible at the time the previous object was clicked.
            if (current.lazyJumpDistance == 0.0 &&
                current.opacityAt(prev.obj.startTime + prev.obj.timePreempt, mods) == 0.0 &&
                prev.startTime + prev.timePreempt > current.startTime) {
                // Perfect stacks are harder the less time between notes.
                hiddenDifficulty += (HIDDEN_MULTIPLIER * 1303) / current.strainTime.pow(1.5)
            }
        }

        // Arbitrary curve for the base value preempt difficulty should have as approach rate increases.
        // https://www.desmos.com/calculator/urjnl7sau7
        val preemptDifficulty =
            ((PREEMPT_STARTING_POINT - current.timePreempt + abs(current.timePreempt - PREEMPT_STARTING_POINT)) / 2).pow(2.35) /
            PREEMPT_BALANCING_FACTOR *
            constantAngleNerfFactor *
            velocityFactor

        var sliderDifficulty = 0.0

        if (current.obj is Slider) {
            val scalingFactor = 50 / current.obj.difficultyRadius

            // Invert the scaling factor to determine the true travel distance independent of circle size.
            val pixelTravelDistance = current.lazyTravelDistance / scalingFactor
            val currentVelocity = pixelTravelDistance / current.travelTime
            val spanTravelDistance = pixelTravelDistance / current.obj.spanCount

            sliderDifficulty +=
                // Reward sliders based on velocity, while also avoiding overbuffing extremely fast sliders.
                min(4.0, currentVelocity * 0.8) *
                // Longer sliders require more reading.
                (spanTravelDistance / 125)

            var cumulativeStrainTime = 0.0

            // Reward for velocity changes based on last few sliders.
            for (i in 0 until min(current.index, 4)) {
                val last = current.previous(i) as? DroidDifficultyHitObject ?: break

                cumulativeStrainTime += last.strainTime

                if (
                    last.obj !is Slider ||
                    // Exclude overlapping objects that can be tapped at once.
                    last.isOverlapping(true)
                ) {
                    continue
                }

                // Invert the scaling factor to determine the true travel distance independent of circle size.
                val lastPixelTravelDistance = last.lazyTravelDistance / scalingFactor
                val lastVelocity = lastPixelTravelDistance / last.travelTime
                val lastSpanTravelDistance = lastPixelTravelDistance / last.obj.spanCount

                sliderDifficulty +=
                    // Reward past sliders based on velocity changes, while also
                    // avoiding overbuffing extremely fast velocity changes.
                    min(4.0, 0.8 * abs(currentVelocity - lastVelocity)) *
                    // Longer sliders require more reading.
                    (lastSpanTravelDistance / 150) *
                    // Avoid overbuffing past sliders.
                    min(1.0, 250 / cumulativeStrainTime)
            }
        }

        return noteDensityDifficulty + hiddenDifficulty + preemptDifficulty + sliderDifficulty
    }

    /**
     * Retrieves a list of objects that are visible at the point in time the current object needs to be hit.
     *
     * @param current The current object.
     */
    private fun retrievePastVisibleObjects(current: DroidDifficultyHitObject) = sequence {
        for (i in 0 until current.index) {
            val prev = current.previous(i) as? DroidDifficultyHitObject ?: break

            if (current.startTime - prev.startTime > READING_WINDOW_SIZE ||
                // The previous object is not visible at the time the current object needs to be hit.
                prev.startTime + prev.timePreempt < current.startTime) {
                break
            }

            if (prev.isOverlapping(true)) {
                continue
            }

            yield(prev)
        }
    }

    /**
     * Calculates the density of objects visible at the point in time the current object needs to be hit.
     *
     * @param current The current object.
     */
    private fun calculateCurrentVisibleObjectsDensity(current: DroidDifficultyHitObject): Double {
        var visibleObjectCount = 0.0
        var index = 0
        var next = current.next(index) as? DroidDifficultyHitObject

        while (next != null) {
            val timeDifference = next.startTime - current.startTime

            if (timeDifference > READING_WINDOW_SIZE ||
                // The next object is not visible at the time the current object needs to be hit.
                current.startTime + current.timePreempt < next.startTime) {
                break
            }

            if (next.isOverlapping(true)) {
                continue
            }

            val timeNerfFactor = getTimeNerfFactor(timeDifference)

            visibleObjectCount += next.opacityAt(current.obj.startTime, EMPTY_MODS) * timeNerfFactor

            next = current.next(++index) as? DroidDifficultyHitObject
        }

        return visibleObjectCount
    }

    /**
     * Returns the time an object spends invisible with the Hidden mod at the current approach rate.
     *
     * @param current The current object.
     */
    private fun getDurationSpentInvisible(current: DroidDifficultyHitObject): Double {
        val obj = current.obj

        val fadeOutStartTime = obj.startTime - obj.timePreempt + obj.timeFadeIn
        val fadeOutDuration = obj.timePreempt * ModHidden.FADE_OUT_DURATION_MULTIPLIER

        return fadeOutStartTime + fadeOutDuration - (obj.startTime - obj.timePreempt)
    }

    /**
     * Calculates a factor of how often the current object's angle has been repeated in a certain time frame.
     * It does this by checking the difference in angle between current and past objects and sums them up
     * based on a range of similarity.
     *
     * @param current The current object.
     */
    private fun getConstantAngleNerfFactor(current: DroidDifficultyHitObject): Double {
        val maxTimeLimit = 2000.0 // 2 seconds
        val minTimeLimit = 200.0

        var constantAngleCount = 0.0
        var index = 0
        var currentTimeGap = 0.0

        while (currentTimeGap < maxTimeLimit) {
            val loopObj = current.previous(index) ?: break

            if (loopObj.angle != null && current.angle != null) {
                val angleDifference = abs(current.angle!! - loopObj.angle!!)

                // Account less for objects that are close to the time limit.
                val longIntervalFactor = (1 - (loopObj.strainTime - minTimeLimit) / (maxTimeLimit - minTimeLimit)).coerceIn(0.0, 1.0)

                constantAngleCount += cos(3 * min(Math.PI / 6, angleDifference)) * longIntervalFactor
            }

            currentTimeGap = current.startTime - loopObj.startTime
            index++
        }

        return (2 / constantAngleCount).coerceIn(0.2, 1.0)
    }

    private fun getTimeNerfFactor(deltaTime: Double) = (2 - deltaTime / (READING_WINDOW_SIZE / 2)).coerceIn(0.0, 1.0)
}