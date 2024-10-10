package com.rian.osu.difficulty.evaluators

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.StandardDifficultyHitObject
import kotlin.math.*

/**
 * An evaluator for calculating osu!standard rhythm difficulty.
 */
object StandardRhythmEvaluator {
    private const val HISTORY_TIME_MAX = 5 * 1000 // 5 seconds of calculateRhythmBonus max.
    private const val HISTORY_OBJECTS_MAX = 32
    private const val RHYTHM_OVERALL_MULTIPLIER = 0.95
    private const val RHYTHM_RATIO_MULTIPLIER = 12.0

    /**
     * Calculates a rhythm multiplier for the difficulty of the tap associated
     * with historic data of the current object.
     *
     * @param current The current object.
     */
    fun evaluateDifficultyOf(current: StandardDifficultyHitObject): Double {
        if (current.obj is Spinner) {
            return 0.0
        }

        var rhythmComplexitySum = 0.0

        val deltaDifferenceEpsilon = current.fullGreatWindow * 0.3

        var island = Island(deltaDifferenceEpsilon)
        var previousIsland = Island(deltaDifferenceEpsilon)
        val islandCounts = mutableMapOf<Island, Int>()

        // Store the ratio of the current start of an island to buff for tighter rhythms.
        var startRatio = 0.0
        var firstDeltaSwitch = false
        var rhythmStart = 0
        val historicalNoteCount = min(current.index, HISTORY_OBJECTS_MAX)

        while (rhythmStart < historicalNoteCount - 2 &&
            current.startTime - current.previous(rhythmStart)!!.startTime < HISTORY_TIME_MAX
        ) {
            ++rhythmStart
        }

        for (i in rhythmStart downTo 1) {
            val currentObject = current.previous(i - 1)!!
            val prevObject = current.previous(i)!!
            val lastObject = current.previous(i + 1)!!

            // Scale note 0 to 1 from history to now.
            val timeDecay = (HISTORY_TIME_MAX - (current.startTime - currentObject.startTime)) / HISTORY_TIME_MAX
            val noteDecay = (historicalNoteCount - i).toDouble() / historicalNoteCount

            // Either we're limited by time or limited by object count.
            val currentHistoricalDecay = min(noteDecay, timeDecay)

            val currentDelta = currentObject.strainTime
            val prevDelta = prevObject.strainTime
            val lastDelta = lastObject.strainTime

            // Calculate how much current delta difference deserves a rhythm bonus
            // This function is meant to reduce rhythm bonus for deltas that are multiples of each other (i.e. 100 and 200)
            val deltaDifferenceRatio = min(prevDelta, currentDelta) / max(prevDelta, currentDelta)

            val currentRatio = 1 + RHYTHM_RATIO_MULTIPLIER * min(0.5, sin(Math.PI / deltaDifferenceRatio).pow(2.0))

            // Reduce ratio bonus if delta difference is too big
            val fraction = max(prevDelta / currentDelta, currentDelta / prevDelta)
            val fractionMultiplier = (2 - fraction / 8).coerceIn(0.0, 1.0)

            val windowPenalty =
                ((abs(prevDelta - currentDelta) - deltaDifferenceEpsilon) / deltaDifferenceEpsilon).coerceIn(0.0, 1.0)

            var effectiveRatio = windowPenalty * currentRatio * fractionMultiplier

            if (firstDeltaSwitch) {
                if (abs(prevDelta - currentDelta) < deltaDifferenceEpsilon) {
                    // Island is still progressing, count size.
                    island.addDelta(currentDelta.toInt())
                } else {
                    // BPM change is into slider, this is easy acc window.
                    if (currentObject.obj is Slider) {
                        effectiveRatio /= 8
                    }

                    // Bpm change was from a slider, this is easier typically than circle -> circle
                    // Unintentional side effect is that bursts with kicksliders at the ends might have lower difficulty
                    // than bursts without sliders
                    if (prevObject.obj is Slider) {
                        effectiveRatio *= 0.3
                    }

                    // Repeated island polarity (2 -> 4, 3 -> 5)
                    if (island.isSimilarPolarity(previousIsland)) {
                        effectiveRatio /= 2
                    }

                    // Previous increase happened a note ago.
                    // Albeit this is a 1/1 -> 1/2-1/4 type of transition, we don't want to buff this.
                    if (lastDelta > prevDelta - deltaDifferenceEpsilon && prevDelta > currentDelta - deltaDifferenceEpsilon) {
                        effectiveRatio /= 8
                    }

                    // Repeated island size (ex: triplet -> triplet)
                    // TODO: remove this nerf since its staying here only for balancing purposes because of the flawed ratio calculation
                    if (previousIsland.deltaCount == island.deltaCount) {
                        effectiveRatio /= 2
                    }

                    if (island in islandCounts) {
                        // Only add island to island counts if they're going one after another.
                        if (previousIsland == island) {
                            islandCounts[island] = islandCounts[island]!! + 1
                        }

                        // Repeated island (ex: triplet -> triplet)
                        effectiveRatio *= min(
                            3.0 / islandCounts[island]!!,
                            (1.0 / islandCounts[island]!!).pow(2.75 / (1 + exp(14 - 0.24 * island.delta)))
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
            } else if (prevDelta > currentDelta + deltaDifferenceEpsilon) {
                // We are speeding up.
                // Begin counting island until we change speed again.
                firstDeltaSwitch = true

                // BPM change is into slider, this is easy acc window
                if (currentObject.obj is Slider)
                    effectiveRatio *= 0.6

                // BPM change was from a slider, this is easier typically than circle -> circle
                // Unintentional side effect is that bursts with kicksliders at the ends might have lower difficulty
                // than bursts without sliders
                if (prevObject.obj is Slider)
                    effectiveRatio *= 0.6

                startRatio = effectiveRatio

                island = Island(currentDelta.toInt(), deltaDifferenceEpsilon)
            }
        }

        return sqrt(4 + rhythmComplexitySum * RHYTHM_OVERALL_MULTIPLIER) / 2
    }
}

private class Island(epsilon: Double) {
    private val deltaDifferenceEpsilon = epsilon

    var delta = Int.MAX_VALUE
        private set(value) {
            if (field == Int.MAX_VALUE) {
                field = max(value, DifficultyHitObject.MIN_DELTA_TIME)
            }

            ++deltaCount
        }

    var deltaCount = 0
        private set

    constructor(delta: Int, deltaDifferenceEpsilon: Double) : this(deltaDifferenceEpsilon) {
        this.delta = max(delta, DifficultyHitObject.MIN_DELTA_TIME)
    }

    fun addDelta(delta: Int) {
        this.delta = delta
    }

    fun isSimilarPolarity(other: Island) =
        // TODO: consider islands to be of similar polarity only if they're having the same average delta (we don't want to consider 3 singletaps similar to a triple)
        // naively adding delta check here breaks _a lot_ of maps because of the flawed ratio calculation
        deltaCount % 2 == other.deltaCount % 2

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Island) {
            return false
        }

        return abs(delta - other.delta) < deltaDifferenceEpsilon && deltaCount == other.deltaCount
    }

    override fun hashCode() = super.hashCode()
}
