package com.rian.osu.replay

import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.math.Vector2
import com.rian.osu.mods.ModHardRock
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayMovement
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

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
     * The version of the replay.
     */
    private val replayVersion: Int,

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
    private val mehWindow = beatmap.hitWindow.mehWindow
    private val isHardRock = difficultyAttributes.mods.any { it is ModHardRock }

    /**
     * Checks if relevant [Slider]s in the [DroidPlayableBeatmap] are cheesed and computes the penalties.
     */
    fun calculatePenalty(): SliderCheesePenalty {
        if (
            difficultyAttributes.difficultSliders.isEmpty() ||
            difficultyAttributes.aimSliderFactor == 1.0
        ) {
            return SliderCheesePenalty()
        }

        val summedDifficultyRating = min(1.0, getCheesedDifficultyRatings().sum())

        return SliderCheesePenalty(computePenalty(difficultyAttributes.aimSliderFactor, summedDifficultyRating))
    }

    private fun getCheesedDifficultyRatings(): List<Double> {
        val objects = beatmap.hitObjects.objects
        val cheesedDifficultyRatings = mutableListOf<Double>()

        // Current loop indices are stored for efficiency.
        val cursorLoopIndices = IntArray(cursorGroups.size) { 0 }

        val objectRadius = objects.first().difficultyRadius
        val objectRadiusSquared = objectRadius * objectRadius
        val sliderBallRadius = objectRadius * 2
        val sliderBallRadiusSquared = sliderBallRadius * sliderBallRadius

        // Sort difficult sliders by index so that cursor loop indices work properly.
        for (difficultSlider in difficultyAttributes.difficultSliders.sortedBy { it.index }) {
            if (difficultSlider.index >= objectData.size) {
                continue
            }

            val slider = objects[difficultSlider.index] as Slider
            val objData = objectData[difficultSlider.index]

            val lateHitThreshold = if (replayVersion >= 8) mehWindow else min(mehWindow, slider.duration)

            // If a miss or slider break occurs, we disregard the check for that slider.
            if (objData.tickSet == null || objData.result == ResultType.MISS.id ||
                -mehWindow > objData.accuracy || objData.accuracy > lateHitThreshold) {
                continue
            }

            val sliderStartPosition = slider.difficultyStackedPosition
            val sliderStartX = sliderStartPosition.x
            val sliderStartY = sliderStartPosition.y

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
                var closestDistanceSquared = Float.POSITIVE_INFINITY
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
                        val distanceSquared = Vector2.distanceSquared(getMovementX(group.down), getMovementY(group.down), sliderStartX, sliderStartY)

                        if (closestDistanceSquared > distanceSquared) {
                            closestDistanceSquared = distanceSquared
                            closestIndex = j
                        }

                        if (closestDistanceSquared <= objectRadiusSquared) {
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

                        var distanceSquared = Float.POSITIVE_INFINITY
                        val movementX = getMovementX(movement)
                        val movementY = getMovementY(movement)
                        val prevMovementX = getMovementX(prevMovement)
                        val prevMovementY = getMovementY(prevMovement)

                        when (movement.touchType) {
                            TouchType.UP -> {
                                distanceSquared = Vector2.distanceSquared(prevMovementX, prevMovementY, sliderStartX, sliderStartY)
                            }

                            TouchType.MOVE -> {
                                var mSecPassed = max(prevMovement.time.toDouble(), minTimeLimit)
                                val maxTime = min(movement.time.toDouble(), maxTimeLimit)

                                // Iterate every 1ms.
                                while (mSecPassed <= maxTime) {
                                    val t = (mSecPassed.toFloat() - prevMovement.time) /
                                        (movement.time - prevMovement.time)
                                    val interpolatedX = prevMovementX + (movementX - prevMovementX) * t
                                    val interpolatedY = prevMovementY + (movementY - prevMovementY) * t

                                    distanceSquared = Vector2.distanceSquared(interpolatedX, interpolatedY, sliderStartX, sliderStartY)

                                    if (closestDistanceSquared > distanceSquared) {
                                        closestDistanceSquared = distanceSquared
                                        closestIndex = j
                                    }

                                    if (closestDistanceSquared <= objectRadiusSquared) {
                                        break
                                    }

                                    ++mSecPassed
                                }
                            }

                            else -> Unit
                        }

                        if (closestDistanceSquared > distanceSquared) {
                            closestDistanceSquared = distanceSquared
                            closestIndex = j
                        }

                        if (closestDistanceSquared <= objectRadiusSquared) {
                            break
                        }
                    }

                    cursorLoopIndices[i] = ++j
                }

                closestDistances.add(closestDistanceSquared)
                closestGroupIndices.add(closestIndex)

                if (cursorLoopIndices[i] > 0) {
                    // Decrement the index. The previous group may also have a role on the next slider.
                    --cursorLoopIndices[i]
                }
            }

            val groupsIndex = closestDistances.indexOf(closestDistances.min())
            val closestDistanceSquared = closestDistances[groupsIndex]

            if (closestDistanceSquared > objectRadiusSquared) {
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
                val nestedX = nestedPosition.x
                val nestedY = nestedPosition.y

                while (occurrenceLoopIndex < movements.size && movements[occurrenceLoopIndex].time < nestedObject.startTime) {
                    ++occurrenceLoopIndex
                }

                if (occurrenceLoopIndex >= movements.size) {
                    continue
                }

                val movement = movements[occurrenceLoopIndex]
                val prevMovement = movements[occurrenceLoopIndex - 1]
                val movementX = getMovementX(movement)
                val movementY = getMovementY(movement)
                val prevMovementX = getMovementX(prevMovement)
                val prevMovementY = getMovementY(prevMovement)

                isCheesed = when (movement.touchType) {
                    TouchType.MOVE -> {
                        // Interpolate cursor position during nested object time.
                        val t = (nestedObject.startTime.toFloat() - prevMovement.time) /
                            (movement.time - prevMovement.time)
                        val interpolatedX = prevMovementX + (movementX - prevMovementX) * t
                        val interpolatedY = prevMovementY + (movementY - prevMovementY) * t

                        Vector2.distanceSquared(interpolatedX, interpolatedY, nestedX, nestedY) > sliderBallRadiusSquared
                    }

                    TouchType.UP -> Vector2.distanceSquared(prevMovementX, prevMovementY, nestedX, nestedY) > sliderBallRadiusSquared

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

    private fun getMovementX(movement: ReplayMovement) = movement.x

    private fun getMovementY(movement: ReplayMovement) = if (isHardRock) 512 - movement.y else movement.y
}