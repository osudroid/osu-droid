package com.rian.osu.replay

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.HitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.beatmap.timings.BreakPeriod
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.difficulty.attributes.HighStrainSection
import com.rian.osu.math.Interpolation
import com.rian.osu.math.Vector2
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModPrecise
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.TouchType
import kotlin.math.abs
import kotlin.math.pow

/**
 * Utility to check whether a [Beatmap] is three-fingered in a replay.
 */
class ThreeFingerChecker(
    /**
     * The [Beatmap] to check.
     */
    @JvmField
    val beatmap: Beatmap,

    /**
     * The [DroidDifficultyAttributes] of the [Beatmap].
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
     * The ratio threshold between non-three finger cursors and three-finger cursors.
     *
     * Increasing this number will increase detection accuracy, however
     * it also increases the chance of falsely flagged plays.
     */
    private val threeFingerRatioThreshold = 0.01

    /**
     * Extended sections of the [Beatmap] for drag detection.
     */
    private val beatmapSections = mutableListOf<ThreeFingerBeatmapSection>()

    /**
     * The [HitWindow] of the [Beatmap].
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

    init {
        createBeatmapSections()
    }

    /**
     * Calculates the three-finger penalty of the replay.
     *
     * The [Beatmap] will be separated into sections, where each section will be determined whether it is dragged.
     *
     * After that, each section will be assigned a "nerf factor" based on whether the section is three-fingered. These
     * nerf factors will be summed up into a final nerf factor, taking the [Beatmap]'s difficulty into account.
     */
    fun calculatePenalty(): Double {
        if (difficultyAttributes.possibleThreeFingeredSections.isEmpty()) {
            return 1.0
        }

        if (validCursorGroups.count { it.isNotEmpty() } <= 3) {
            return 1.0
        }

        val strainFactors = mutableListOf<Double>()
        val fingerFactors = mutableListOf<Double>()
        val lengthFactors = mutableListOf<Double>()

        for (section in beatmapSections) {
            val cursorCounts = mutableListOf<Int>()

            for (i in validCursorGroups.indices) {
                var count = 0

                for (obj in section.objects) {
                    if (obj.pressingCursorInstanceIndex == section.dragFingerIndex || obj.pressingCursorIndex == -1) {
                        continue
                    }

                    ++count
                }

                cursorCounts.add(count)
            }

            if (section.dragFingerIndex != -1) {
                // Remove the drag index to prevent it from being picked up into the detection.
                cursorCounts.removeAt(section.dragFingerIndex)
            }

            // This index will be used to detect if a section is 3-fingered.
            // If the section is dragged, the dragged instance will be ignored,
            // hence why the index is 1 less than non-dragged section.
            val fingerSplitIndex = if (section.dragFingerIndex != -1) 2 else 3

            // Divide >=4th (3rd for drag) cursor instances with 1st + 2nd (+ 3rd for non-drag)
            // to check if the section is 3-fingered.
            val threeFingeredObjectCount = cursorCounts.subList(fingerSplitIndex, cursorCounts.size).sum()
            val threeFingerRatio = threeFingeredObjectCount /
                    cursorCounts.subList(0, fingerSplitIndex).sum().coerceAtLeast(1).toDouble()

            if (threeFingerRatio > threeFingerRatioThreshold) {
                val sectionObjectCount = section.lastObjectIndex - section.firstObjectIndex + 1
                val threeFingeredObjectRatio = threeFingeredObjectCount / sectionObjectCount.toDouble()

                // We can ignore the first 3 (2 for drag) filled cursor instances
                // since they are guaranteed not 3 finger.
                val threeFingerCursorCounts = cursorCounts.subList(fingerSplitIndex, cursorCounts.size)
                    .filter { it > 0 }

                // Finger factor applies more penalty if more fingers were used.
                fingerFactors.add(threeFingerCursorCounts.foldIndexed(1.0) { index, acc, count ->
                    acc + ((index + 1) * count * threeFingeredObjectRatio).pow(0.9)
                })

                // Length factor applies more penalty if there are more 3-fingered object.
                lengthFactors.add(1 + threeFingeredObjectRatio.pow(1.2))

                // Strain factor applies more penalty if the section is more difficult.
                strainFactors.add(section.sumStrain * threeFingeredObjectRatio)
            }
        }

        return strainFactors.foldIndexed(1.0) { index, acc, strainFactor ->
            acc + 0.015 * (strainFactor * fingerFactors[index] * lengthFactors[index]).pow(1.05)
        }
    }

    private fun createAccurateBreakPeriods() = mutableListOf<BreakPeriod>().apply {
        val objects = beatmap.hitObjects.objects

        for (breakPeriod in beatmap.events.breaks) {
            val beforeIndex = (objects.indexOfFirst { it.getEndTime() >= breakPeriod.startTime } - 1)
                .coerceIn(0, objects.size - 2)

            val objectBefore = objects[beforeIndex]
            val objectBeforeData = objectData[beforeIndex]
            var timeBefore = objectBefore.getEndTime()

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

    private fun filterCursorGroups() = mutableListOf<MutableList<CursorGroup>>().apply {
        val firstObject = beatmap.hitObjects.objects.first()
        val firstObjectResult = objectData.first()

        val lastObject = beatmap.hitObjects.objects.last()
        val lastObjectResult = objectData.last()

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
    }

    private fun createBeatmapSections() {
        val cursorLookupIndices = mutableListOf<Int>()
        for (i in validCursorGroups.indices) {
            cursorLookupIndices.add(0)
        }

        for (section in difficultyAttributes.possibleThreeFingeredSections) {
            val dragFingerIndex = findDragIndex(section)
            val objects = mutableListOf<ThreeFingerObject>()

            for (i in section.firstObjectIndex..section.lastObjectIndex) {
                val objPressIndices = getObjectPressIndex(
                    beatmap.hitObjects.objects[i], objectData[i], cursorLookupIndices, dragFingerIndex
                )

                objects.add(
                    ThreeFingerObject(
                        beatmap.hitObjects.objects[i],
                        objPressIndices.first,
                        objPressIndices.second
                    )
                )
            }

            beatmapSections.add(ThreeFingerBeatmapSection(section, objects, dragFingerIndex))
        }
    }

    private fun findDragIndex(section: HighStrainSection): Int {
        val firstObject = beatmap.hitObjects.objects[section.firstObjectIndex]
        val lastObject = beatmap.hitObjects.objects[section.lastObjectIndex]

        var firstObjectMinHitTime = firstObject.startTime

        if (firstObject is HitCircle) {
            firstObjectMinHitTime -= when (objectData[section.firstObjectIndex].result) {
                ResultType.HIT300.id -> hitWindow.greatWindow
                ResultType.HIT100.id -> hitWindow.okWindow
                else -> hitWindow.mehWindow
            }
        } else if (firstObject is Slider) {
            firstObjectMinHitTime -= hitWindow.mehWindow
        }

        var lastObjectMaxHitTime = lastObject.startTime

        if (lastObject is HitCircle) {
            lastObjectMaxHitTime += when (objectData[section.lastObjectIndex].result) {
                ResultType.HIT300.id -> hitWindow.greatWindow
                ResultType.HIT100.id -> hitWindow.okWindow
                else -> hitWindow.mehWindow
            }
        } else if (lastObject is Slider) {
            lastObjectMaxHitTime += hitWindow.mehWindow.coerceAtMost(lastObject.spanDuration.toFloat())
        }

        // Since there may be more than 1 cursor instance index,
        // we check which cursor instance follows hit objects all over.
        val cursorIndices = mutableListOf<Int>()

        for (i in validCursorGroups.indices) {
            val groups = validCursorGroups[i]

            if (groups.isEmpty()) {
                continue
            }

            // Do not include cursors that don't have an occurrence in this section.
            // This speeds up checking process.
            if (groups.none { it.startTime >= firstObjectMinHitTime && it.endTime <= lastObjectMaxHitTime }) {
                continue
            }

            // If this cursor instance doesn't move, it's not the cursor instance we want.
            if (groups.none { it.moves.isNotEmpty() }) {
                continue
            }

            cursorIndices.add(i)
        }

        val sectionObjects = beatmap.hitObjects.objects.subList(section.firstObjectIndex, section.lastObjectIndex + 1)
        val sectionObjectData = objectData.sliceArray(section.firstObjectIndex..section.lastObjectIndex)

        for (i in sectionObjects.indices) {
            val obj = sectionObjects[i]
            val objData = sectionObjectData[i]

            if (obj is Spinner || objData.result == ResultType.MISS.id) {
                continue
            }

            // Exclude slider breaks.
            if (obj is Slider && objData.accuracy == (hitWindow.mehWindow + 13).toInt().toShort()) {
                continue
            }

            val objPosition = obj.stackedPosition
            val hitTime = obj.startTime + objData.accuracy

            // Observe the cursor position at the object's hit time.
            for (j in cursorIndices.indices) {
                val cursorIndex = cursorIndices[j]

                if (cursorIndex == -1) {
                    continue
                }

                val group = validCursorGroups[cursorIndex].find { it.isActiveAt(hitTime) } ?: continue
                val movements = group.allMovements
                var isInObject = false

                for (k in 1 until movements.size) {
                    val movement = movements[k]
                    val prevMovement = movements[k - 1]

                    val movementPosition = Vector2(movement.point)
                    val prevMovementPosition = Vector2(prevMovement.point)

                    // Only consider cursor at interval prev.time <= hitTime <= current.time.
                    if (prevMovement.time <= hitTime && hitTime <= movement.time) {
                        isInObject = when (movement.touchType) {
                            TouchType.UP -> movementPosition.getDistance(prevMovementPosition) <= obj.radius

                            TouchType.MOVE -> {
                                // Interpolate movement.
                                val t =
                                    ((hitTime - prevMovement.time) / (movement.time - prevMovement.time)).toFloat()
                                val cursorPosition = Vector2(
                                    Interpolation.linear(
                                        prevMovementPosition.x,
                                        movementPosition.x,
                                        t
                                    ),
                                    Interpolation.linear(
                                        prevMovementPosition.y,
                                        movementPosition.y,
                                        t
                                    )
                                )

                                cursorPosition.getDistance(objPosition) <= obj.radius
                            }

                            else -> break
                        }

                        break
                    }
                }

                if (!isInObject) {
                    cursorIndices[j] = -1
                }
            }
        }

        return cursorIndices.indexOfFirst { it != -1 }
    }

    private fun getObjectPressIndex(
        obj: HitObject,
        objData: ReplayObjectData,
        cursorLookupIndices: MutableList<Int>,
        vararg excludedCursorIndices: Int
    ): Pair<Int, Int> {
        if (obj is Spinner || objData.result == ResultType.MISS.id) {
            return -1 to -1
        }

        // Check for slider breaks and treat them as misses.
        if (obj is Slider && objData.accuracy == (hitWindow.mehWindow + 13).toInt().toShort()) {
            return -1 to -1
        }

        // We are not directly using hit time to determine which cursor pressed the object
        // to account for time difference between hit registration and object judgement.
        var minHitTime = obj.startTime
        var maxHitTime = obj.startTime

        if (obj is HitCircle) {
            val hitWindowGap = when (objData.result) {
                ResultType.HIT300.id -> hitWindow.greatWindow
                ResultType.HIT100.id -> hitWindow.okWindow
                else -> hitWindow.mehWindow
            }

            minHitTime -= hitWindowGap
            maxHitTime += hitWindowGap
        } else if (obj is Slider) {
            minHitTime -= hitWindow.mehWindow
            maxHitTime += hitWindow.mehWindow.coerceAtMost(obj.spanDuration.toFloat())
        }

        val hitTime = obj.startTime + objData.accuracy
        var nearestCursorInstanceIndex: Int? = null
        var nearestCursorIndex: Int? = null
        var nearestTime = Double.POSITIVE_INFINITY

        for (i in validCursorGroups.indices) {
            if (excludedCursorIndices.contains(i)) {
                continue
            }

            val groups = validCursorGroups[i]
            var j = cursorLookupIndices[i]

            while (j < groups.size) {
                val cursor = groups[j].down

                if (cursor.time < minHitTime) {
                    cursorLookupIndices[i] = ++j
                    continue
                }

                if (cursor.time > maxHitTime) {
                    break
                }

                val deltaTime = abs(cursor.time - hitTime)
                if (deltaTime > nearestTime) {
                    break
                }

                nearestCursorInstanceIndex = i
                nearestCursorIndex = j
                nearestTime = deltaTime

                cursorLookupIndices[i] = ++j
            }
        }

        return (nearestCursorInstanceIndex ?: -1) to (nearestCursorIndex ?: -1)
    }
}