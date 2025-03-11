package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModTraceable
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

/**
 * An evaluator for calculating osu!droid visual difficulty.
 */
object DroidVisualEvaluator {
    /**
     * Evaluates the difficulty of reading the current object, based on:
     *
     * - note density of the current object,
     * - overlapping factor of the current object,
     * - the preempt time of the current object,
     * - the visual opacity of the current object,
     * - the velocity of the current object if it's a slider,
     * - past objects' velocity if they are sliders,
     * - whether the Hidden mod is enabled,
     * - and whether the Traceable mod is enabled.
     *
     * @param current The current object.
     * @param mods The mods used.
     * @param withSliders Whether to take slider difficulty into account.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, mods: List<Mod>, withSliders: Boolean): Double {
        if (
            current.obj is Spinner ||
            // Exclude overlapping objects that can be tapped at once.
            current.isOverlapping(true) ||
            current.index == 0
        ) {
            return 0.0
        }

        // Start with base density and give global bonus for Hidden and Traceable.
        // Add density caps for sanity.
        var strain = when {
            mods.any { it is ModHidden } -> min(30.0, current.noteDensity.pow(3))

            mods.any { it is ModTraceable } ->
                // Give more bonus for hit circles due to there being no circle piece.
                if (current.obj is HitCircle) min(25.0, current.noteDensity.pow(2.5))
                else min(22.5, current.noteDensity.pow(2.25))

            else -> min(20.0, current.noteDensity.pow(2))
        }

        for (i in 0 until min(current.index, 10)) {
            val previous = (current.previous(i) ?: break) as DroidDifficultyHitObject

            if (
                previous.obj is Spinner ||
                // Exclude overlapping objects that can be tapped at once.
                previous.isOverlapping(true)
            ) {
                continue
            }

            // Do not consider objects that don't fall under time preempt.
            if (
                current.startTime - previous.endTime >
                current.timePreempt
            ) {
                break
            }

            strain += (1 - current.opacityAt(previous.obj.startTime, mods)) / 4
        }

        if (current.timePreempt < 400) {
            // Give bonus for AR higher than 10.33.
            strain += (400 - current.timePreempt).pow(1.35) / 100
        }

        // Scale the value with overlapping factor.
        strain /= 10 * (1 + current.overlappingFactor)

        if (current.obj is Slider && withSliders) {
            val scalingFactor = 50 / current.obj.difficultyRadius

            // Invert the scaling factor to determine the true travel distance independent of circle size.
            val pixelTravelDistance = current.obj.lazyTravelDistance / scalingFactor
            val currentVelocity = pixelTravelDistance / current.travelTime
            val spanTravelDistance = pixelTravelDistance / current.obj.spanCount

            strain +=
                // Reward sliders based on velocity, while also avoiding overbuffing extremely fast sliders.
                min(6.0, currentVelocity * 1.5) *
                // Longer sliders require more reading.
                (spanTravelDistance / 100)

            var cumulativeStrainTime = 0.0

            // Reward for velocity changes based on last few sliders.
            for (i in 0 until min(current.index, 4)) {
                val last = (current.previous(i) ?: break) as DroidDifficultyHitObject

                cumulativeStrainTime += last.strainTime

                if (
                    last.obj !is Slider ||
                    // Exclude overlapping objects that can be tapped at once.
                    last.isOverlapping(true)
                ) {
                    continue
                }

                // Invert the scaling factor to determine the true travel distance independent of circle size.
                val lastPixelTravelDistance = last.obj.lazyTravelDistance / scalingFactor
                val lastVelocity = lastPixelTravelDistance / last.travelTime
                val lastSpanTravelDistance = lastPixelTravelDistance / last.obj.spanCount

                strain +=
                    // Reward past sliders based on velocity changes, while also
                    // avoiding overbuffing extremely fast velocity changes.
                    min(10.0, 2.5 * abs(currentVelocity - lastVelocity)) *
                    // Longer sliders require more reading.
                    (lastSpanTravelDistance / 125) *
                    // Avoid overbuffing past sliders.
                    min(1.0, 300 / cumulativeStrainTime)
            }
        }

        return strain
    }
}