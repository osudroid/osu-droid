package com.rian.osu.replay

import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.math.Interpolation
import com.rian.osu.math.Vector2
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModPrecise
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.TouchType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayMovement

/**
 * Utility to check whether relevant [Slider]s in a [DroidPlayableBeatmap] are cheesed.
 */
class SliderCheeseChecker(
    /**
     * The [DroidPlayableBeatmap] to check.
     */
    @JvmField
    val beatmap: DroidPlayableBeatmap,

    /**
     * The [DroidDifficultyAttributes] of the [DroidPlayableBeatmap].
     */
    @JvmField
    val difficultyAttributes: DroidDifficultyAttributes,

    /**
     * Cursors that are grouped together in the form of [CursorGroup]s.
     *
     * Each index represents a cursor index.
     */
    private val cursorGroups: List<List<CursorGroup>>,

    /**
     * Data regarding [HitObject] information.
     */
    private val objectData: Array<Replay.ReplayObjectData>
) {
    private val mehWindow = (
        if (difficultyAttributes.mods.any { it is ModPrecise }) PreciseDroidHitWindow(beatmap.difficulty.od)
        else DroidHitWindow(beatmap.difficulty.od)
    ).mehWindow

    /**
     * Checks if relevant [Slider]s in the [DroidPlayableBeatmap] are cheesed and computes the penalties.
     */
    fun calculatePenalty(): SliderCheesePenalty {
        if (
            difficultyAttributes.difficultSliders.isEmpty() ||
            doubleArrayOf(
                difficultyAttributes.aimSliderFactor,
                difficultyAttributes.flashlightSliderFactor,
                difficultyAttributes.visualSliderFactor
            ).all { it == 1.0 }
        ) {
            return SliderCheesePenalty()
        }

        val summedDifficultyRating = min(1.0, getCheesedDifficultyRatings().sum())

        return SliderCheesePenalty(
            computePenalty(difficultyAttributes.aimSliderFactor, summedDifficultyRating),
            computePenalty(difficultyAttributes.flashlightSliderFactor, summedDifficultyRating),
            computePenalty(difficultyAttributes.visualSliderFactor, summedDifficultyRating)
        )
    }

    private fun getCheesedDifficultyRatings(): List<Double> {
        val objects = beatmap.hitObjects.objects
        val cheesedDifficultyRatings = mutableListOf<Double>()

        // Current loop indices are stored for efficiency.
        val cursorLoopIndices = IntArray(cursorGroups.size) { 0 }

        val objectRadius = objects.first().difficultyRadius
        val sliderBallRadius = objectRadius * 2

        // Sort difficult sliders by index so that cursor loop indices work properly.
        for (difficultSlider in difficultyAttributes.difficultSliders.sortedBy { it.index }) {
            if (difficultSlider.index >= objectData.size) {
                continue
            }

            val objData = objectData[difficultSlider.index]

            // If a miss or slider break occurs, we disregard the check for that slider.
            if (objData.tickSet == null || objData.result == ResultType.MISS.id || objData.accuracy == (mehWindow + 13).toInt().toShort()) {
                continue
            }

            val slider = objects[difficultSlider.index] as Slider
            val sliderStartPosition = slider.difficultyStackedPosition

            // These time boundaries should consider the delta time between the previous and next
            // object as well as their hit accuracy. However, they are somewhat complicated to
            // compute and the accuracy gain is small. As such, let's settle with 50 hit window.
            val minTimeLimit = slider.startTime - mehWindow
            val maxTimeLimit = slider.startTime + mehWindow

            // Get the closest tap distance across all cursors.
            val closestDistances = mutableListOf<Float>()
            val closestGroupIndices = mutableListOf<Int>()

            for (i in cursorGroups.indices) {
                val groups = cursorGroups[i]
                var closestDistance = Float.POSITIVE_INFINITY
                var closestIndex = groups.size
                var j = cursorLoopIndices[i]

                while (j < groups.size) {
                    val group = groups[j]

                    if (group.endTime < minTimeLimit) {
                        cursorLoopIndices[i] = ++j
                        continue
                    }

                    if (group.startTime > maxTimeLimit) {
                        break
                    }

                    if (group.startTime >= minTimeLimit) {
                        val distance = getMovementPosition(group.down).getDistance(sliderStartPosition)

                        if (closestDistance > distance) {
                            closestDistance = distance
                            closestIndex = j
                        }

                        if (closestDistance <= objectRadius) {
                            break
                        }
                    }

                    // Normally, we check if there are cursor presses within the group's active time.
                    // However, some funky workarounds are used throughout the game for replays, so
                    // for the time being we only check for cursor distances across the group.
                    val movements = group.allMovements

                    for (k in 1 until movements.size) {
                        val movement = movements[k]
                        val prevMovement = movements[k - 1]

                        var distance = Float.POSITIVE_INFINITY

                        when (movement.touchType) {
                            TouchType.UP -> distance = getMovementPosition(prevMovement).getDistance(sliderStartPosition)

                            TouchType.MOVE -> {
                                var mSecPassed = max(prevMovement.time.toDouble(), minTimeLimit)
                                val maxTime = min(movement.time.toDouble(), maxTimeLimit)

                                // Iterate every 1ms.
                                while (mSecPassed <= maxTime) {
                                    val t = (mSecPassed.toFloat() - prevMovement.time) /
                                        (movement.time - prevMovement.time)

                                    val interpolatedPosition = Interpolation.linear(
                                        getMovementPosition(prevMovement),
                                        getMovementPosition(movement),
                                        t
                                    )

                                    distance = interpolatedPosition.getDistance(sliderStartPosition)

                                    if (closestDistance > distance) {
                                        closestDistance = distance
                                        closestIndex = j
                                    }

                                    if (closestDistance <= objectRadius) {
                                        break
                                    }

                                    ++mSecPassed
                                }
                            }

                            else -> Unit
                        }

                        if (closestDistance > distance) {
                            closestDistance = distance
                            closestIndex = j
                        }

                        if (closestDistance <= objectRadius) {
                            break
                        }
                    }

                    cursorLoopIndices[i] = ++j
                }

                closestDistances.add(closestDistance)
                closestGroupIndices.add(closestIndex)

                if (cursorLoopIndices[i] > 0) {
                    // Decrement the index. The previous group may also have a role on the next slider.
                    --cursorLoopIndices[i]
                }
            }

            val groupsIndex = closestDistances.indexOf(closestDistances.min())
            val closestDistance = closestDistances[groupsIndex]

            if (closestDistance > objectRadius) {
                // The closest cursor is outside the slider's head.
                cheesedDifficultyRatings.add(difficultSlider.difficultyRating)
                continue
            }

            // Track cursor movement to see if it lands on every tick.
            val group = cursorGroups[groupsIndex][closestGroupIndices[groupsIndex]]
            var isCheesed = false
            var occurrenceLoopIndex = 1
            val movements = group.allMovements

            for (i in 1 until slider.nestedHitObjects.size) {
                if (isCheesed) {
                    break
                }

                val tickWasHit = objData.tickSet[i - 1]
                if (!tickWasHit) {
                    continue
                }

                val nestedObject = slider.nestedHitObjects[i]
                val nestedPosition = nestedObject.difficultyStackedPosition

                while (occurrenceLoopIndex < movements.size && movements[occurrenceLoopIndex].time < nestedObject.startTime) {
                    ++occurrenceLoopIndex
                }

                if (occurrenceLoopIndex >= movements.size) {
                    continue
                }

                val movement = movements[occurrenceLoopIndex]
                val prevMovement = movements[occurrenceLoopIndex - 1]

                isCheesed = when (movement.touchType) {
                    TouchType.MOVE -> {
                        // Interpolate cursor position during nested object time.
                        val t = (nestedObject.startTime.toFloat() - prevMovement.time) /
                            (movement.time - prevMovement.time)

                        val interpolatedPosition = Interpolation.linear(
                            getMovementPosition(prevMovement),
                            getMovementPosition(movement),
                            t
                        )

                        interpolatedPosition.getDistance(nestedPosition) > sliderBallRadius
                    }

                    TouchType.UP -> getMovementPosition(prevMovement).getDistance(nestedPosition) > sliderBallRadius

                    else -> false
                }
            }

            if (isCheesed) {
                cheesedDifficultyRatings.add(difficultSlider.difficultyRating)
            }
        }

        return cheesedDifficultyRatings
    }

    private fun computePenalty(factor: Double, ratingSum: Double) = max(factor, (1 - ratingSum * factor).pow(2))

    private fun getMovementPosition(movement: ReplayMovement) =
        if (difficultyAttributes.mods.any { it is ModHardRock }) Vector2(movement.point.x, 512 - movement.point.y)
        else Vector2(movement.point)
}