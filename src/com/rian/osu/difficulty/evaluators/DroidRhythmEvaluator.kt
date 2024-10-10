package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.DroidDifficultyHitObject
import kotlin.math.*

/**
 * An evaluator for calculating osu!droid rhythm difficulty.
 */
object DroidRhythmEvaluator {
    private class Island(epsilon: Double) {
        val deltas = mutableListOf<Int>()
        private val deltaDifferenceEpsilon = epsilon

        constructor(firstDelta: Int, epsilon: Double) : this(epsilon) {
            addDelta(firstDelta)
        }

        fun addDelta(delta: Int) {
            val existingDelta = deltas.firstOrNull { abs(it - delta) >= deltaDifferenceEpsilon }

            deltas.add(existingDelta ?: delta)
        }

        val averageDelta by lazy {
            if (deltas.isNotEmpty()) max(deltas.average(), DifficultyHitObject.MIN_DELTA_TIME.toDouble())
            else 0.0
        }

        fun isSimilarPolarity(other: Island) =
            // Consider islands to be of similar polarity only if they're having the same average delta
            // (we don't want to consider 3 singletaps similar to a triple).
            abs(averageDelta - other.averageDelta) < deltaDifferenceEpsilon && deltas.size % 2 == other.deltas.size % 2

        override fun hashCode() =
            // We need to compare all deltas, and they must be in the exact same order we added them
            deltas.joinToString(separator = "").hashCode()

        override fun equals(other: Any?) = other?.hashCode() == hashCode()
    }

    private const val RHYTHM_MULTIPLIER = 1.2
    private const val MAX_ISLAND_SIZE = 7
    private const val HISTORY_OBJECTS_MAX = 24
    private const val HISTORY_TIME_MAX = 4000 // 4 seconds of calculateRhythmBonus max.

    /**
     * Calculates a rhythm multiplier for the difficulty of the tap associated
     * with historic data of the current object.
     *
     * @param current The current object.
     */
    @JvmStatic
    fun evaluateDifficultyOf(current: DroidDifficultyHitObject): Double {
        if (
            current.obj is Spinner ||
            // Exclude overlapping objects that can be tapped at once.
            current.isOverlapping(false)
        ) {
            return 1.0
        }

        val deltaDifferenceEpsilon = current.fullGreatWindow * 0.3
        var rhythmComplexitySum = 0.0

        var island = Island(deltaDifferenceEpsilon)
        var previousIsland = Island(deltaDifferenceEpsilon)
        val islandCounts = mutableMapOf<Island, Int>()

        // Store the ratio of the current start of an island to buff for tighter rhythms.
        var startRatio = 0.0
        var firstDeltaSwitch = false
        var rhythmStart = 0

        val historicalNoteCount = min(current.index, HISTORY_OBJECTS_MAX)

        // Exclude overlapping objects that can be tapped at once.
        val validPrevious = mutableListOf<DroidDifficultyHitObject>()

        for (i in 0 until historicalNoteCount) {
            (current.previous(i) as DroidDifficultyHitObject?)?.apply {
                if (!isOverlapping(false)) {
                    validPrevious.add(this)
                }
            } ?: break
        }

        while (
            rhythmStart < validPrevious.size - 2 &&
            current.startTime - validPrevious[rhythmStart].startTime < HISTORY_TIME_MAX
        ) {
            ++rhythmStart
        }

        for (i in rhythmStart downTo 1) {
            val currentObject = current.previous(i - 1)!!
            val prevObject = current.previous(i)!!
            val lastObject = current.previous(i + 1)!!

            // Scale note 0 to 1 from history to now.
            var currentHistoricalDecay =
                (HISTORY_TIME_MAX - (current.startTime - currentObject.startTime)) / HISTORY_TIME_MAX

            // Either we're limited by time or limited by object count.
            currentHistoricalDecay =
                min(currentHistoricalDecay, (historicalNoteCount - i).toDouble() / historicalNoteCount)

            val currentDelta = currentObject.strainTime
            val prevDelta = prevObject.strainTime
            val lastDelta = lastObject.strainTime

            val currentRatio = 1 + 10 * min(
                0.5,
                sin(Math.PI / (min(prevDelta, currentDelta) / max(prevDelta, currentDelta))).pow(2)
            )

            val windowPenalty =
                ((abs(prevDelta - currentDelta) - deltaDifferenceEpsilon) / deltaDifferenceEpsilon).coerceIn(0.0, 1.0)

            var effectiveRatio = windowPenalty * currentRatio

            if (firstDeltaSwitch) {
                if (abs(prevDelta - currentDelta) <= deltaDifferenceEpsilon) {
                    if (island.deltas.size < MAX_ISLAND_SIZE) {
                        // Island is still progressing.
                        island.addDelta(currentDelta.toInt())
                    }
                } else {
                    if (currentObject.obj is Slider) {
                        // BPM change is into slider, this is easy acc window.
                        effectiveRatio /= 8
                    }

                    // BPM change was from a slider, this is typically easier than circle -> circle.
                    // Unintentional side effect is that bursts with kick-sliders at the ends might
                    // have lower difficulty than bursts without sliders.
                    if (prevObject.obj is Slider) {
                        effectiveRatio /= 4
                    }

                    // Repeated island polarity (2 -> 4, 3 -> 5).
                    if (island.isSimilarPolarity(previousIsland)) {
                        effectiveRatio *= 0.3
                    }

                    // Previous increase happened a note ago.
                    // Albeit this is a 1/1 -> 1/2-1/4 type of transition, we don't want to buff this.
                    if (lastDelta > prevDelta + deltaDifferenceEpsilon &&
                        prevDelta > currentDelta + deltaDifferenceEpsilon) {
                        effectiveRatio /= 8
                    }

                    // Singletaps are easier to control.
                    if (island.deltas.size == 1) {
                        effectiveRatio /= 2
                    }

                    if (island in islandCounts) {
                        // Only add island to island counts if they're going one after another.
                        if (previousIsland == island) {
                            islandCounts[island] = islandCounts[island]!! + 1
                        }

                        // Repeated island (ex: triplet -> triplet).
                        // Graph: https://www.desmos.com/calculator/pj7an56zwf
                        effectiveRatio *= min(
                            1.0 / islandCounts[island]!!,
                            (1.0 / islandCounts[island]!!).pow(4 / (1 + exp(10 - 0.165 * island.averageDelta)))
                        )
                    } else {
                        islandCounts[island] = 1
                    }

                    // Scale down the difficulty if the object is doubletappable.
                    effectiveRatio *= 1 - prevObject.doubletapness * 0.75

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
            } else if (prevDelta > deltaDifferenceEpsilon + currentDelta) {
                // We're speeding up.
                // Begin counting island until we change speed again.
                firstDeltaSwitch = true

                // Reduce ratio if we're starting after a slider.
                if (prevObject.obj is Slider) {
                    effectiveRatio *= 0.3
                }

                startRatio = effectiveRatio
                island = Island(currentDelta.toInt(), deltaDifferenceEpsilon)
            }
        }

        // Nerf doubles that can be tapped at the same time to get Great hit results.
        val doubletapness = 1 - current.doubletapness

        return sqrt(4 + rhythmComplexitySum * RHYTHM_MULTIPLIER * doubletapness) / 2
    }
}