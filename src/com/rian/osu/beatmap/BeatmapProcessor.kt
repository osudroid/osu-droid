package com.rian.osu.beatmap

import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.mods.Mod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Provides functionality to alter an [IBeatmap] after it has been converted.
 */
class BeatmapProcessor @JvmOverloads constructor(
    /**
     * The [IBeatmap] to process. This should already be converted to the applicable mode.
     */
    @JvmField
    val beatmap: IBeatmap,

    /**
     * The [CoroutineScope] to use for coroutines.
     */
    private val scope: CoroutineScope? = null
) {
    /**
     * Processes the converted [IBeatmap] prior to [HitObject.applyDefaults] being invoked.
     *
     * Nested [HitObject]s generated during [HitObject.applyDefaults] will not be present by this point,
     * and no [Mod]s will have been applied to the [HitObject]s.
     *
     * This can only be used to add alterations to [HitObject]s generated directly through the conversion process.
     */
    fun preProcess() {
        var lastObj: HitObject? = null

        beatmap.hitObjects.objects.forEach {
            scope?.ensureActive()
            it.updateComboInformation(lastObj)
            lastObj = it
        }

        scope?.ensureActive()

        // Mark the last object in the beatmap as last in combo.
        if (lastObj != null) {
            lastObj.isLastInCombo = true
        }
    }

    /**
     * Processes the converted [IBeatmap] after [HitObject.applyDefaults] has been invoked.
     *
     * Nested [HitObject]s generated during [HitObject.applyDefaults] will be present by this point,
     * and [Mod]s will have been applied to all [HitObject]s.
     *
     * This should be used to add alterations to [HitObject]s while they are in their most playable state.
     */
    fun postProcess() = beatmap.hitObjects.run {
        if (objects.isEmpty()) {
            return@run
        }

        // Recount slider ticks as nested hit objects are only generated after HitObject.applyDefaults.
        sliderTickCount = 0

        forEach {
            scope?.ensureActive()

            if (it is Slider) {
                sliderRepeatCount += it.repeatCount
                sliderTickCount += it.nestedHitObjects.size - 2 - it.repeatCount
            }

            // Reset stacking
            it.difficultyStackHeight = 0
            it.gameplayStackHeight = 0
        }

        if (beatmap.formatVersion >= 6) {
            applyStacking()
        } else {
            applyStackingOld()
        }
    }

    private fun applyStacking() {
        val objects = beatmap.hitObjects.objects
        val startIndex = 0
        val endIndex = objects.size - 1
        var extendedEndIndex = endIndex

        if (endIndex < objects.size - 1) {
            scope?.ensureActive()

            // Extend the end index to include objects they are stacked on
            for (i in endIndex downTo startIndex) {
                scope?.ensureActive()

                var stackBaseIndex = i

                for (n in stackBaseIndex + 1 until objects.size) {
                    scope?.ensureActive()

                    val stackBaseObject = objects[stackBaseIndex]
                    if (stackBaseObject is Spinner) {
                        break
                    }

                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    val endTime = stackBaseObject.endTime
                    val stackThreshold = calculateStackThreshold(beatmap, objectN)

                    if (objectN.startTime - endTime > stackThreshold) {
                        // We are no longer within stacking range of the next object.
                        break
                    }

                    if (stackBaseObject.position.getDistance(objectN.position) < STACK_DISTANCE ||
                        stackBaseObject is Slider && stackBaseObject.endPosition.getDistance(objectN.position) < STACK_DISTANCE
                    ) {
                        stackBaseIndex = n

                        // HitObjects after the specified update range haven't been reset yet
                        objectN.difficultyStackHeight = 0
                        objectN.gameplayStackHeight = 0
                    }
                }

                if (stackBaseIndex > extendedEndIndex) {
                    extendedEndIndex = stackBaseIndex

                    if (extendedEndIndex == objects.size - 1) {
                        break
                    }
                }
            }
        }

        // Reverse pass for stack calculation.
        for (i in extendedEndIndex downTo startIndex + 1) {
            scope?.ensureActive()

            var n = i

            // We should check every note which has not yet got a stack.
            // Consider the case we have two inter-wound stacks and this will make sense.
            //
            // o <-1      o <-2
            //  o <-3      o <-4
            //
            // We first process starting from 4 and handle 2,
            // then we come backwards on the i-th loop iteration until we reach 3 and handle 1.
            // 2 and 1 will be ignored in the i-th loop because they already have a stack value.
            var objectI = objects[i]
            if (objectI.difficultyStackHeight != 0 || objectI is Spinner) {
                continue
            }

            val stackThreshold = calculateStackThreshold(beatmap, objectI)

            // If this object is a hit circle, then we enter this "special" case.
            // It either ends with a stack of hit circles only, or a stack of hit circles that are underneath a slider.
            // Any other case is handled by the "is Slider" code below this.
            if (objectI is HitCircle) {
                while (--n >= 0) {
                    scope?.ensureActive()

                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    // Truncation to integer is required to match osu!stable - both quantities being subtracted there
                    // are integers.
                    if (objectI.startTime.toInt() - objectN.endTime.toInt() > stackThreshold) {
                        // We are no longer within stacking range of the previous object.
                        break
                    }

                    // Hit objects before the specified update range haven't been reset yet
                    objectN.difficultyStackHeight = 0
                    objectN.gameplayStackHeight = 0

                    // This is a special case where hit circles are moved DOWN and RIGHT (negative stacking) if they are under the *last* slider in a stacked pattern.
                    // o==o <- slider is at original location
                    //     o <- hitCircle has stack of -1
                    //      o <- hitCircle has stack of -2
                    if (objectN is Slider && objectN.endPosition.getDistance(objectI.position) < STACK_DISTANCE) {
                        val offset = objectI.difficultyStackHeight - objectN.difficultyStackHeight + 1

                        for (j in n + 1..i) {
                            scope?.ensureActive()

                            // For each object which was declared under this slider, we will offset it to appear *below* the slider end (rather than above).
                            val objectJ = objects[j]

                            if (objectN.endPosition.getDistance(objectJ.position) < STACK_DISTANCE) {
                                objectJ.difficultyStackHeight -= offset
                                objectJ.gameplayStackHeight -= offset
                            }
                        }

                        // We have hit a slider. We should restart calculation using this as the new base.
                        // Breaking here will mean that the slider still has a stack count of 0, so will be handled in the i-outer-loop.
                        break
                    }

                    if (objectN.position.getDistance(objectI.position) < STACK_DISTANCE) {
                        // Keep processing as if there are no sliders. If we come across a slider, this gets cancelled out.
                        // NOTE: Sliders with start positions stacking are a special case that is also handled here.
                        objectN.difficultyStackHeight = objectI.difficultyStackHeight + 1
                        objectN.gameplayStackHeight = objectI.gameplayStackHeight + 1
                        objectI = objectN
                    }
                }
            } else if (objectI is Slider) {
                // We have hit the first slider in a possible stack.
                // From this point on, we ALWAYS stack positive regardless.
                while (--n >= startIndex) {
                    scope?.ensureActive()

                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    if (objectI.startTime - objectN.startTime > stackThreshold) {
                        // We are no longer within stacking range of the previous object.
                        break
                    }

                    if (objectN.endPosition.getDistance(objectI.position) < STACK_DISTANCE) {
                        objectN.difficultyStackHeight = objectI.difficultyStackHeight + 1
                        objectN.gameplayStackHeight = objectI.gameplayStackHeight + 1
                        objectI = objectN
                    }
                }
            }
        }
    }

    private fun applyStackingOld() {
        val objects = beatmap.hitObjects.objects

        for (i in objects.indices) {
            scope?.ensureActive()

            val currentObject = objects[i]
            if (currentObject.difficultyStackHeight != 0 && currentObject !is Slider) {
                continue
            }

            var sliderStack = 0
            var startTime = currentObject.endTime
            val stackThreshold = calculateStackThreshold(beatmap, currentObject)

            for (j in i + 1 until objects.size) {
                scope?.ensureActive()

                if (objects[j].startTime - stackThreshold > startTime) {
                    break
                }

                // Note the use of `startTime` in the code below doesn't match osu!stable's use of `endTime`.
                // This is because in osu!stable's implementation, `UpdateCalculations` is not called on the inner-loop hit object (j)
                // and therefore it does not have a correct `endTime`, but instead the default of `endTime = startTime`.
                //
                // Effects of this can be seen on https://osu.ppy.sh/beatmapsets/243#osu/1146 at sliders around 86647 ms, where
                // if we use `endTime` here it would result in unexpected stacking.
                //
                // Reference: https://github.com/ppy/osu/pull/24188
                if (objects[j].position.getDistance(currentObject.position) < STACK_DISTANCE) {
                    ++currentObject.difficultyStackHeight
                    ++currentObject.gameplayStackHeight
                    startTime = objects[j].startTime
                } else if (objects[j].position.getDistance(currentObject.endPosition) < STACK_DISTANCE) {
                    // Case for sliders - bump notes down and right, rather than up and left.
                    ++sliderStack
                    objects[j].difficultyStackHeight -= sliderStack
                    objects[j].gameplayStackHeight -= sliderStack
                    startTime = objects[j].startTime
                }
            }
        }
    }

    companion object {
        private const val STACK_DISTANCE = 3

        /**
         * Truncation of [HitObject.timePreempt] to [Int], as well as keeping the result as [Float], are both done for
         * the purposes of osu!stable compatibility.
         *
         * Note that top-level objects [HitObject.timePreempt] is supposed to be integral anyway; see
         * [HitObject.applyDefaults] using [com.rian.osu.beatmap.sections.BeatmapDifficulty.difficultyRangeInt] when
         * calculating it.
         *
         * Slider ticks and end circles are the exception to that, but they do not matter for stacking.
         */
        private fun calculateStackThreshold(beatmap: IBeatmap, hitObject: HitObject) =
            hitObject.timePreempt.toInt() * beatmap.general.stackLeniency
    }
}