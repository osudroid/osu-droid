package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import kotlin.math.*

/**
 * An evaluator for calculating osu!droid rhythm difficulty.
 */
object DroidRhythmEvaluator {
    private const val HISTORY_TIME_MAX = 5 * 1000 // 5 seconds of calculateRhythmBonus max.
    private const val HISTORY_OBJECTS_MAX = 32
    private const val RHYTHM_OVERALL_MULTIPLIER = 0.8
    private const val RHYTHM_RATIO_MULTIPLIER = 32.0

    /**
     * Calculates a rhythm multiplier for the difficulty of the tap associated
     * with historic data of the current object.
     *
     * @param current The current object.
     * @param useSliderAccuracy Whether to use slider accuracy.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject, useSliderAccuracy: Boolean): Double {
        if (current.obj is Spinner) {
            return 1.0
        }

        if (current.index <= 1) {
            return 1.0
        }

        var rhythmComplexitySum = 0.0

        val deltaDifferenceEpsilon = current.fullGreatWindow * 0.3

        var island = Island(deltaDifferenceEpsilon)
        var previousIsland = Island(deltaDifferenceEpsilon)
        val islandCounts = mutableListOf<IslandCounter>()

        // Store the ratio of the current start of an island to buff for tighter rhythms.
        var startRatio = 0.0
        var firstDeltaSwitch = false
        var rhythmStart = 0

        val historicalNoteCount = min(current.index, HISTORY_OBJECTS_MAX)

        // Exclude overlapping objects that can be tapped at once.
        val validPrevious = mutableListOf<DroidDifficultyHitObject>()

        for (i in 0 until historicalNoteCount) {
            val prev = current.previous(i) as? DroidDifficultyHitObject ?: break

            if (!prev.isOverlapping(false)) {
                validPrevious.add(prev)
            }
        }

        if (validPrevious.size < 3) {
            return 1.0
        }

        while (
            rhythmStart < validPrevious.size - 2 &&
            current.startTime - validPrevious[rhythmStart].startTime < HISTORY_TIME_MAX
        ) {
            ++rhythmStart
        }

        var prevObject = validPrevious[rhythmStart]
        var lastObject = validPrevious[rhythmStart + 1]

        for (i in rhythmStart downTo 1) {
            val currentObject = validPrevious[i - 1]

            // Scale note 0 to 1 from history to now.
            val timeDecay = (HISTORY_TIME_MAX - (current.startTime - currentObject.startTime)) / HISTORY_TIME_MAX
            val noteDecay = (validPrevious.size - i).toDouble() / validPrevious.size

            // Either we're limited by time or limited by object count.
            val currentHistoricalDecay = min(noteDecay, timeDecay)

            // Use custom cap value to ensure that at this point delta time is actually zero.
            val currentDelta = currentObject.deltaTime.coerceAtLeast(1e-7)
            val prevDelta = prevObject.deltaTime.coerceAtLeast(1e-7)
            val lastDelta = lastObject.deltaTime.coerceAtLeast(1e-7)

            // Calculate how much current delta difference deserves a rhythm bonus
            // This function is meant to reduce rhythm bonus for deltas that are multiples of each other (i.e. 100 and 200)
            val deltaDifference = max(prevDelta, currentDelta) / min(prevDelta, currentDelta)

            // Reduce ratio bonus if delta difference is too big
            val differenceMultiplier = (2 - deltaDifference / 8).coerceIn(0.0, 1.0)

            val windowPenalty =
                ((abs(prevDelta - currentDelta) - deltaDifferenceEpsilon) / deltaDifferenceEpsilon).coerceIn(0.0, 1.0)

            var effectiveRatio = windowPenalty * getEffectiveRatio(deltaDifference) * differenceMultiplier

            // If the previous object is a slider, it might be easier to tap since you do not have to do a whole tapping motion.
            // While a full deltatime might end up some weird ratio, the "unpress->tap" motion might be simple.
            // For example, a slider-circle-circle pattern should be evaluated as a regular triple and not as a single->double.
            if (prevObject.obj is Slider) {
                val sliderLazyEndDelta = currentObject.minimumJumpTime
                val sliderLazyEndDeltaDifference = max(sliderLazyEndDelta, currentDelta) / min(sliderLazyEndDelta, currentDelta)

                val sliderRealEndDelta = currentObject.lastObjectEndDeltaTime
                val sliderRealEndDeltaDifference = max(sliderRealEndDelta, currentDelta) / min(sliderRealEndDelta, currentDelta)

                val sliderEffectiveRatio = min(
                    getEffectiveRatio(sliderLazyEndDeltaDifference),
                    getEffectiveRatio(sliderRealEndDeltaDifference)
                )

                effectiveRatio = min(sliderEffectiveRatio, effectiveRatio)
            }

            if (firstDeltaSwitch) {
                if (abs(prevDelta - currentDelta) < deltaDifferenceEpsilon) {
                    // Island is still progressing, count size.
                    island.addDelta(currentDelta.toInt())
                } else {
                    // BPM change is into slider, this is easy acc window.
                    if (!useSliderAccuracy && currentObject.obj is Slider) {
                        effectiveRatio /= 2
                    }

                    // Repeated island polarity (2 -> 4, 3 -> 5)
                    if (island.isSimilarPolarity(previousIsland)) {
                        effectiveRatio /= 2
                    }

                    // Previous increase happened a note ago.
                    // Albeit this is a 1/1 -> 1/2-1/4 type of transition, we don't want to buff this.
                    if (lastDelta > prevDelta + deltaDifferenceEpsilon && prevDelta > currentDelta + deltaDifferenceEpsilon) {
                        effectiveRatio /= 8
                    }

                    // Repeated island size (ex: triplet -> triplet)
                    // TODO: remove this nerf since its staying here only for balancing purposes because of the flawed ratio calculation
                    if (previousIsland.deltaCount == island.deltaCount) {
                        effectiveRatio /= 2
                    }

                    var islandFound = false

                    for (counter in islandCounts) {
                        if (island != counter.island) {
                            continue
                        }

                        islandFound = true

                        // Only add island to island counts if they're going one after another.
                        if (previousIsland == island) {
                            counter.count++
                        }

                        // Repeated island (ex: triplet -> triplet)
                        effectiveRatio *= min(
                            3.0 / counter.count,
                            (1.0 / counter.count).pow(
                                DifficultyCalculationUtils.logistic(island.delta.toDouble(), 58.33, 0.24, 2.75)
                            )
                        )

                        break
                    }

                    if (!islandFound) {
                        islandCounts.add(IslandCounter(island, 1))
                    }

                    // Scale down the difficulty if the object is doubletappable.
                    effectiveRatio *= 1 - prevObject.getDoubletapness(currentObject) * 0.75

                    rhythmComplexitySum += sqrt(effectiveRatio * startRatio) * currentHistoricalDecay

                    startRatio = effectiveRatio
                    previousIsland = island

                    if (prevDelta + deltaDifferenceEpsilon < currentDelta) {
                        // We're slowing down, stop counting.
                        // If we're speeding up, this stays as is, and we keep counting island size.
                        firstDeltaSwitch = false
                    }

                    island = Island(currentDelta.toInt(), deltaDifferenceEpsilon)
                }
            } else if (prevDelta > currentDelta + deltaDifferenceEpsilon) {
                // We are speeding up.
                // Begin counting island until we change speed again.
                firstDeltaSwitch = true

                // BPM change is into slider, this is easy acc window.
                if (currentObject.obj is Slider) {
                    effectiveRatio *= 0.6
                }

                // BPM change was from a slider, this is easier typically than circle -> circle
                // Unintentional side effect is that bursts with kicksliders at the ends might have lower difficulty
                // than bursts without sliders
                if (prevObject.obj is Slider) {
                    effectiveRatio *= 0.6
                }

                startRatio = effectiveRatio
                island = Island(currentDelta.toInt(), deltaDifferenceEpsilon)
            }

            lastObject = prevObject
            prevObject = currentObject
        }

        return sqrt(4 + rhythmComplexitySum * RHYTHM_OVERALL_MULTIPLIER) / 2
    }

    private fun getEffectiveRatio(deltaDifference: Double): Double {
        // Take only the fractional part of the value since we are only interested in punishing multiples
        val deltaDifferenceFraction = deltaDifference - truncate(deltaDifference)

        return 1 + RHYTHM_RATIO_MULTIPLIER * min(0.5, DifficultyCalculationUtils.smoothstepBellCurve(deltaDifferenceFraction))
    }
}
