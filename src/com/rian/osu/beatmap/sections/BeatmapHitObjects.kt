package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider

/**
 * Contains information about hit objects of a beatmap.
 */
open class BeatmapHitObjects {
    /**
     * All objects in this beatmap.
     */
    @JvmField
    val objects = mutableListOf<HitObject>()

    /**
     * The amount of circles in this beatmap.
     */
    var circleCount = 0
        private set

    /**
     * The amount of sliders in this beatmap.
     */
    var sliderCount = 0
        private set

    /**
     * The amount of spinners in this beatmap.
     */
    var spinnerCount = 0
        private set

    /**
     * Adds hit objects to this beatmap.
     *
     * @param objects The hit objects to add.
     */
    fun add(objects: Iterable<HitObject>) = objects.forEach { add(it) }

    /**
     * Adds a hit object to this beatmap.
     *
     * @param obj The hit object to add.
     */
    open fun add(obj: HitObject) {
        // Objects may be out of order *only* if a user has manually edited an .osu file.
        // Unfortunately there are "ranked" maps in this state (example: https://osu.ppy.sh/s/594828).
        // Finding index is used to guarantee that the parsing order of hit objects with equal start times is maintained (stably-sorted).
        objects.add(findInsertionIndex(obj.startTime), obj)

        when (obj) {
            is HitCircle -> ++circleCount
            is Slider -> ++sliderCount
            else -> ++spinnerCount
        }
    }

    /**
     * Removes a hit object from this beatmap.
     *
     * @param `object` The hit object to remove.
     * @return Whether the hit object was successfully removed.
     */
    open fun remove(obj: HitObject): Boolean {
        val removed = objects.remove(obj)

        if (removed) {
            when (obj) {
                is HitCircle -> --circleCount
                is Slider -> --sliderCount
                else -> --spinnerCount
            }
        }

        return removed
    }

    /**
     * Removes a hit object from this beatmap at a given index.
     *
     * @param index The index of the hit object to remove.
     * @return The hit object that was removed, `null` if no hit objects were removed.
     */
    open fun remove(index: Int): HitObject? {
        if (index < 0 || index > objects.size - 1) {
            return null
        }

        return objects.removeAt(index).also {
            when (it) {
                is HitCircle -> --circleCount
                is Slider -> --sliderCount
                else -> --spinnerCount
            }
        }
    }

    /**
     * Clears all hit objects from this beatmap.
     */
    fun clear() {
        objects.clear()
        circleCount = 0
        sliderCount = 0
        spinnerCount = 0
    }

    /**
     * Finds the insertion index of a hit object in a given time.
     *
     * @param startTime The start time of the hit object.
     */
    private fun findInsertionIndex(startTime: Double): Int {
        if (objects.size == 0 || startTime < objects[0].startTime) {
            return 0
        }

        if (startTime >= objects[objects.size - 1].startTime) {
            return objects.size
        }

        var l = 0
        var r = objects.size - 2

        while (l <= r) {
            val pivot = l + (r - l shr 1)
            val obj = objects[pivot]
            val objStartTime = obj.startTime

            when {
                objStartTime < startTime -> l = pivot + 1
                objStartTime > startTime -> r = pivot - 1
                else -> return pivot
            }
        }

        return l
    }
}
