package com.rian.osu.replay

import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.HitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.beatmap.timings.BreakPeriod
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.math.Interpolation
import com.rian.osu.math.Vector2
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModPrecise
import kotlin.math.max
import kotlin.math.pow
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayMovement
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

/**
 * Utility to check whether a [DroidPlayableBeatmap] is three-fingered in a replay.
 */
class ThreeFingerChecker(
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
    private val objectData: Array<ReplayObjectData>
) {
    /**
     * The [HitWindow] of the [DroidPlayableBeatmap].
     *
     * Keep in mind that speed-changing [Mod]s do not change hit window length in game logic.
     */
    private val hitWindow =
        if (difficultyAttributes.mods.any { it is ModPrecise }) PreciseDroidHitWindow(beatmap.difficulty.od)
        else DroidHitWindow(beatmap.difficulty.od)

    /**
     * A reprocessed [BreakPeriod]s to match right on [HitObject] time.
     *
     * This is used to increase detection accuracy since [BreakPeriod]s do not start right at the
     * start of the [HitObject] before it and do not end right at the first [HitObject] after it.
     */
    private val accurateBreakPeriods = createAccurateBreakPeriods()

    /**
     * Cursors that are grouped together in the form of [CursorGroup]s that counts towards three-finger detection.
     *
     * Each index represents a cursor index.
     */
    private val validCursorGroups = filterCursorGroups()

    /**
     * Extended sections of the [DroidPlayableBeatmap] for detection.
     */
    private val beatmapSections = createBeatmapSections()

    /**
     * Calculates the three-finger penalty of the replay.
     *
     * The [DroidPlayableBeatmap] will be separated into sections, where each section will be assigned a "nerf factor"
     * based on whether the section is three-fingered. These nerf factors will be summed up into a final nerf factor,
     * taking the [DroidPlayableBeatmap]'s difficulty into account.
     */
    fun calculatePenalty(): Double {
        if (difficultyAttributes.possibleThreeFingeredSections.isEmpty()) {
            return 1.0
        }

        if (validCursorGroups.count { it.isNotEmpty() } <= 3) {
            return 1.0
        }

        var penalty = 1.0

        for (section in beatmapSections) {
            val threeFingerCursorCounts = mutableListOf<Int>()

            for (i in 0 until cursorGroups.size - 2) {
                threeFingerCursorCounts.add(0)
            }

            for (obj in section.objects) {
                if (obj.pressingCursorInstanceIndex == -1) {
                    continue
                }

                if (obj.aimingCursorInstanceIndex < 3) {
                    // The aim cursor is in the first three cursors. They are counted as non-3 finger.
                    when (obj.pressingCursorInstanceIndex) {
                        0, 1, 2 -> break

                        else -> ++threeFingerCursorCounts[obj.pressingCursorInstanceIndex - 3]
                    }
                } else {
                    // The aim cursor is somewhere else. Only count the first 2 cursors as non-3 finger.
                    when (obj.pressingCursorInstanceIndex) {
                        0, 1 -> break

                        else -> ++threeFingerCursorCounts[obj.pressingCursorInstanceIndex - 2]
                    }
                }
            }

            val threeFingerCount = threeFingerCursorCounts.sum()

            if (threeFingerCount == 0) {
                continue
            }

            val sectionObjectCount = section.objects.size
            val threeFingeredObjectRatio = threeFingerCount.toDouble() / sectionObjectCount

            val strainFactor = max(1.0, section.sumStrain * threeFingeredObjectRatio)

            // Finger factor applies more penalty if more fingers were used.
            val fingerFactor = threeFingerCursorCounts.foldIndexed(1.0) { index, acc, count ->
                acc + ((index + 1) * count.toDouble() / sectionObjectCount).pow(0.9)
            }

            // Length factor applies more penalty if there are more 3-fingered object.
            val lengthFactor = 1.0 + threeFingeredObjectRatio.pow(0.8)

            penalty += 0.015 * (strainFactor * fingerFactor * lengthFactor).pow(1.05)
        }

        return penalty
    }

    /**
     * Generates a new set of "accurate break points".
     *
     * This is done to increase detection accuracy since break points do not start right at the
     * end of the object before it and do not end right at the first object after it.
     */
    private fun createAccurateBreakPeriods() = mutableListOf<BreakPeriod>().apply {
        val objects = beatmap.hitObjects.objects

        for (breakPeriod in beatmap.events.breaks) {
            val beforeIndex = (objects.indexOfFirst { it.endTime >= breakPeriod.startTime } - 1)
                .coerceIn(0, objects.size - 2)

            val objectBefore = objects[beforeIndex]
            val objectBeforeData = objectData[beforeIndex]
            var timeBefore = objectBefore.endTime

            if (objectBefore is HitCircle) {
                timeBefore +=
                    if (objectBeforeData.result != ResultType.MISS.id) objectBeforeData.accuracy.toFloat()
                    else hitWindow.mehWindow
            }

            val afterIndex = beforeIndex + 1
            val objectAfter = objects[afterIndex]
            val objectAfterData = objectData[afterIndex]
            var timeAfter = objectAfter.startTime

            if (objectAfter is HitCircle && objectAfterData.result != ResultType.MISS.id) {
                timeAfter += objectAfterData.accuracy
            }

            add(BreakPeriod(timeBefore.toFloat(), timeAfter.toFloat()))
        }
    }

    /**
     * Filters the original cursor instances, returning only those with [TouchType.DOWN] movement.
     *
     * This also filters cursors that are in a [BreakPeriod] or happen before start/after end of the
     * [DroidPlayableBeatmap].
     */
    private fun filterCursorGroups() = mutableListOf<List<CursorGroup>>().apply {
        val objects = beatmap.hitObjects.objects
        val firstObject = objects[0]
        val firstObjectResult = objectData[0]

        val lastObject = objects[objects.size - 1]
        val lastObjectResult = objectData[objectData.size - 1]

        // For sliders, automatically set hit window length to be as lenient as possible.
        var firstObjectHitWindow = hitWindow.mehWindow

        if (firstObject is HitCircle) {
            firstObjectHitWindow = when (firstObjectResult.result) {
                ResultType.HIT300.id -> hitWindow.greatWindow
                ResultType.HIT100.id -> hitWindow.okWindow
                else -> hitWindow.mehWindow
            }
        }

        // For sliders, automatically set hit window length to be as lenient as possible.
        var lastObjectHitWindow = hitWindow.mehWindow

        if (lastObject is HitCircle) {
            lastObjectHitWindow = when (lastObjectResult.result) {
                ResultType.HIT300.id -> hitWindow.greatWindow
                ResultType.HIT100.id -> hitWindow.okWindow
                else -> hitWindow.mehWindow
            }
        } else if (lastObject is Slider) {
            lastObjectHitWindow = lastObjectHitWindow.coerceAtMost(lastObject.spanDuration.toFloat())
        }

        // These hit time uses hit window length as threshold.
        // This is because cursors aren't recorded exactly at hit time.
        val firstObjectHitTime = firstObject.startTime - firstObjectHitWindow
        val lastObjectHitTime = lastObject.startTime + lastObjectHitWindow

        for (i in cursorGroups.indices) {
            val groups = mutableListOf<CursorGroup>()

            for (group in cursorGroups[i]) {
                if (group.startTime < firstObjectHitTime) {
                    continue
                }

                if (group.endTime > lastObjectHitTime) {
                    break
                }

                if (accurateBreakPeriods.any { group.startTime >= it.startTime && group.endTime <= it.endTime }) {
                    continue
                }

                groups.add(group)
            }

            add(groups)
        }
    }.toList()

    /**
     * Divides the beatmap into sections, which will be used to assign presses per object and improve detection speed.
     */
    private fun createBeatmapSections() = mutableListOf<ThreeFingerBeatmapSection>().apply {
        val aimCursorGroupLookupIndices = IntArray(validCursorGroups.size) { 0 }
        // This intentionally starts from 1 because we need to look at the previous cursor.
        val aimCursorLookupIndices = IntArray(validCursorGroups.size) { 1 }
        val pressCursorLookupIndices = IntArray(validCursorGroups.size) { 0 }

        for (section in difficultyAttributes.possibleThreeFingeredSections) {
            val objects = mutableListOf<ThreeFingerObject>()

            for (i in section.firstObjectIndex..section.lastObjectIndex) {
                val obj = beatmap.hitObjects.objects[i]
                val objData = objectData[i]

                val aimIndex = getObjectAimIndex(obj, objData, aimCursorGroupLookupIndices, aimCursorLookupIndices)
                val pressIndex = getObjectPressIndex(obj, objData, pressCursorLookupIndices)

                objects.add(ThreeFingerObject(obj, aimIndex, pressIndex))
            }

            add(ThreeFingerBeatmapSection(section, objects))
        }
    }.toList()

    /**
     * Obtains the index of the cursor that aimed the object at the nearest time.
     *
     * @param obj The object to obtain the index for.
     * @param objData The hit data of the object.
     * @param cursorGroupIndices The cursor indices to start looking for the cursor group from, to save computation time.
     * @param cursorIndices The cursor indices to start looking for the cursor from, to save computation time.
     * @return The index of the cursor. -1 if the object was missed, or it's a spinner.
     */
    private fun getObjectAimIndex(
        obj: HitObject,
        objData: ReplayObjectData,
        cursorGroupIndices: IntArray,
        cursorIndices: IntArray
    ): Int {
        if (objData.result == ResultType.MISS.id || obj is Spinner) {
            return -1
        }

        // Check for slider breaks and treat them as misses.
        if (obj is Slider && objData.accuracy == (hitWindow.mehWindow + 13).toInt().toShort()) {
            return -1
        }

        val hitTime = obj.startTime.toFloat() + objData.accuracy
        val objPosition = obj.difficultyStackedPosition

        // We are maintaining the closest distance to the object.
        // This is because the radius that is calculated is using an estimation.
        // As such, it does not reflect the actual object radius in gameplay.
        var closestDistance = Float.POSITIVE_INFINITY
        var nearestCursorInstanceIndex = -1

        // Observe the cursor position at the object's hit time.
        for (i in cursorGroups.indices) {
            val cursorData = cursorGroups[i]

            for (j in cursorGroupIndices[i] until cursorData.size) {
                val group = cursorData[j]

                if (group.endTime < hitTime) {
                    // Reset cursor index pointer.
                    cursorIndices[i] = 1
                    continue
                }

                if (group.startTime > hitTime) {
                    break
                }

                val movements = group.allMovements

                for (k in cursorIndices[i] until movements.size) {
                    ++cursorIndices[i]

                    val movement = movements[k]
                    val prevMovement = movements[k - 1]

                    // Cursor is past the object's hit time.
                    if (prevMovement.time > hitTime) {
                        break
                    }

                    // Cursor is before the object's hit time.
                    if (hitTime > movement.time) {
                        continue
                    }

                    val movementPosition = getMovementPosition(movement)
                    val prevMovementPosition = getMovementPosition(prevMovement)

                    var distance: Float

                    when (movement.touchType) {
                        TouchType.UP ->
                            distance = prevMovementPosition.getDistance(objPosition)

                        TouchType.MOVE -> {
                            // Interpolate movement.
                            val t = (hitTime - prevMovement.time) / (movement.time - prevMovement.time)
                            val position = Interpolation.linear(prevMovementPosition, movementPosition, t)

                            distance = position.getDistance(objPosition)
                        }

                        else -> continue
                    }

                    if (closestDistance > distance) {
                        closestDistance = distance
                        nearestCursorInstanceIndex = i
                    }
                }
            }
        }

        return nearestCursorInstanceIndex
    }

    /**
     * Obtains the index of the nearest cursor of which an object was pressed in terms of time.
     *
     * @param obj The object to obtain the index for.
     * @param objData The hit data of the object.
     * @param cursorLookupIndices The cursor indices to start looking for the cursor from, to save computation time.
     * @returns The index of the cursor. -1 if the object was missed, or it's a spinner.
     */
    private fun getObjectPressIndex(
        obj: HitObject,
        objData: ReplayObjectData,
        cursorLookupIndices: IntArray
    ): Int {
        if (obj is Spinner || objData.result == ResultType.MISS.id) {
            return -1
        }

        // Check for slider breaks and treat them as misses.
        if (obj is Slider && objData.accuracy == (hitWindow.mehWindow + 13).toInt().toShort()) {
            return -1
        }

        val hitTime = obj.startTime.toFloat() + objData.accuracy
        var nearestCursorInstanceIndex = -1
        var nearestTime = Float.POSITIVE_INFINITY

        for (i in validCursorGroups.indices) {
            val cursorData = validCursorGroups[i]
            var cursorNearestTime = Float.POSITIVE_INFINITY

            for (j in cursorLookupIndices[i] until cursorData.size) {
                ++cursorLookupIndices[i]

                val movement = cursorData[j].down

                if (movement.time > hitTime) {
                    break
                }

                cursorNearestTime = hitTime - movement.time
            }

            if (cursorNearestTime < nearestTime) {
                nearestTime = cursorNearestTime
                nearestCursorInstanceIndex = i
            }
        }

        return nearestCursorInstanceIndex
    }

    private fun getMovementPosition(movement: ReplayMovement) =
        if (difficultyAttributes.mods.any { it is ModHardRock }) Vector2(movement.point.x, 512 - movement.point.y)
        else Vector2(movement.point)
}