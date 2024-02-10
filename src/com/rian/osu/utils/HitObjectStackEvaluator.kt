package com.rian.osu.utils

import com.rian.osu.beatmap.hitobject.*

/**
 * An evaluator for evaluating stack heights of objects.
 */
object HitObjectStackEvaluator {
    private const val STACK_DISTANCE = 3

    /**
     * Applies osu!standard note stacking to hit objects.
     *
     * @param formatVersion The format version of the beatmap containing the hit objects.
     * @param objects       The hit objects to apply stacking to.
     * @param ar            The calculated approach rate of the beatmap.
     * @param stackLeniency The multiplier for the threshold in time where hit objects
     * placed close together stack, ranging from 0 to 1.
     * @param startIndex    The minimum index bound of the hit object to apply stacking to.
     * @param endIndex      The maximum index bound of the hit object to apply stacking to.
     */
    @JvmOverloads
    fun applyStacking(
        formatVersion: Int, objects: List<HitObject>, ar: Float,
        stackLeniency: Float, startIndex: Int = 0, endIndex: Int = objects.size - 1
    ) {
        if (objects.isEmpty()) {
            return
        }

        if (formatVersion < 6) {
            // Use the old version of stacking algorithm for beatmap version 5 or lower.
            applyStackingOld(objects, ar.toDouble(), stackLeniency)
            return
        }

        val timePreempt = (if (ar <= 5) 1800 - 120 * ar else 1950 - 150 * ar).toDouble()
        val stackThreshold = timePreempt * stackLeniency
        var extendedEndIndex = endIndex

        if (endIndex < objects.size - 1) {
            // Extend the end index to include objects they are stacked on
            for (i in endIndex downTo startIndex) {
                var stackBaseIndex = i

                for (n in stackBaseIndex + 1 until objects.size) {
                    val stackBaseObject = objects[stackBaseIndex]
                    if (stackBaseObject is Spinner) {
                        break
                    }

                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    val endTime = stackBaseObject.getEndTime()

                    if (objectN.startTime - endTime > stackThreshold) {
                        // We are no longer within stacking range of the next object.
                        break
                    }

                    if (stackBaseObject.position.getDistance(objectN.position) < STACK_DISTANCE ||
                        stackBaseObject is Slider && stackBaseObject.endPosition.getDistance(objectN.position) < STACK_DISTANCE
                    ) {
                        stackBaseIndex = n

                        // HitObjects after the specified update range haven't been reset yet
                        objectN.stackHeight = 0
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
        var extendedStartIndex = startIndex
        for (i in extendedEndIndex downTo startIndex + 1) {
            var n = i

            // We should check every note which has not yet got a stack.
            // Consider the case we have two inter-wound stacks and this will make sense.
            //
            // o <-1      o <-2
            //  o <-3      o <-4
            //
            // We first process starting from 4 and handle 2,
            // then we come backwards on the i loop iteration until we reach 3 and handle 1.
            // 2 and 1 will be ignored in the i loop because they already have a stack value.
            var objectI = objects[i]
            if (objectI.stackHeight != 0 || objectI is Spinner) {
                continue
            }

            // If this object is a hit circle, then we enter this "special" case.
            // It either ends with a stack of hit circles only, or a stack of hit circles that are underneath a slider.
            // Any other case is handled by the "is Slider" code below this.
            if (objectI is HitCircle) {
                while (--n >= 0) {
                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    if (objectI.startTime - objectN.getEndTime() > stackThreshold) {
                        // We are no longer within stacking range of the previous object.
                        break
                    }

                    // Hit objects before the specified update range haven't been reset yet
                    if (n < extendedStartIndex) {
                        objectN.stackHeight = 0
                        extendedStartIndex = n
                    }

                    // This is a special case where hit circles are moved DOWN and RIGHT (negative stacking) if they are under the *last* slider in a stacked pattern.
                    // o==o <- slider is at original location
                    //     o <- hitCircle has stack of -1
                    //      o <- hitCircle has stack of -2
                    if (objectN is Slider && objectN.endPosition.getDistance(objectI.position) < STACK_DISTANCE) {
                        val offset = objectI.stackHeight - objectN.stackHeight + 1

                        for (j in n + 1..i) {
                            // For each object which was declared under this slider, we will offset it to appear *below* the slider end (rather than above).
                            val objectJ = objects[j]

                            if (objectN.endPosition.getDistance(objectJ.position) < STACK_DISTANCE) {
                                objectJ.stackHeight -= offset
                            }
                        }

                        // We have hit a slider. We should restart calculation using this as the new base.
                        // Breaking here will mean that the slider still has a stack count of 0, so will be handled in the i-outer-loop.
                        break
                    }

                    if (objectN.position.getDistance(objectI.position) < STACK_DISTANCE) {
                        // Keep processing as if there are no sliders. If we come across a slider, this gets cancelled out.
                        // NOTE: Sliders with start positions stacking are a special case that is also handled here.
                        objectN.stackHeight = objectI.stackHeight + 1
                        objectI = objectN
                    }
                }
            } else if (objectI is Slider) {
                // We have hit the first slider in a possible stack.
                // From this point on, we ALWAYS stack positive regardless.
                while (--n >= startIndex) {
                    val objectN = objects[n]
                    if (objectN is Spinner) {
                        continue
                    }

                    if (objectI.startTime - objectN.startTime > stackThreshold) {
                        // We are no longer within stacking range of the previous object.
                        break
                    }

                    if (objectN.getEndPosition().getDistance(objectI.position) < STACK_DISTANCE) {
                        objectN.stackHeight = objectI.stackHeight + 1
                        objectI = objectN
                    }
                }
            }
        }
    }

    /**
     * Applies osu!standard note stacking to hit objects.
     * <br></br><br></br>
     * Used for beatmaps version 5 or older.
     *
     * @param objects The hit objects to apply stacking to.
     * @param ar The calculated approach rate of the beatmap.
     * @param stackLeniency The multiplier for the threshold in time where hit objects
     * placed close together stack, ranging from 0 to 1.
     */
    private fun applyStackingOld(objects: List<HitObject>, ar: Double, stackLeniency: Float) {
        val timePreempt = if (ar <= 5) 1800 - 120 * ar else 1950 - 150 * ar
        val stackThreshold = timePreempt * stackLeniency

        for (i in objects.indices) {
            val currentObject = objects[i]
            if (currentObject.stackHeight != 0 && currentObject !is Slider) {
                continue
            }

            var sliderStack = 0
            var startTime = currentObject.getEndTime()

            for (j in i + 1 until objects.size) {
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
                    ++currentObject.stackHeight
                    startTime = objects[j].startTime
                } else if (objects[j].position.getDistance(currentObject.getEndPosition()) < STACK_DISTANCE) {
                    // Case for sliders - bump notes down and right, rather than up and left.
                    objects[j].stackHeight -= ++sliderStack
                    startTime = objects[j].startTime
                }
            }
        }
    }
}
