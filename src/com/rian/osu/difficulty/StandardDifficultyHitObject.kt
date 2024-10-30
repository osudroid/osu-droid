package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import kotlin.math.min

/**
 * Represents a [HitObject] with additional information for osu!standard difficulty calculation.
 */
class StandardDifficultyHitObject(
    /**
     * The [HitObject] that this [StandardDifficultyHitObject] wraps.
     */
    obj: HitObject,

    /**
     * The [HitObject] that occurs before [obj].
     */
    lastObj: HitObject,

    /**
     * The [HitObject] that occurs before [lastObj].
     */
    lastLastObj: HitObject?,

    /**
     * The clock rate being calculated.
     */
    clockRate: Double,

    /**
     * Other hit objects in the beatmap, including this hit object.
     */
    difficultyHitObjects: Array<StandardDifficultyHitObject>,

    /**
     * The index of this hit object in the list of all hit objects.
     *
     * This is one less than the actual index of the hit object in the beatmap.
     */
    index: Int,

    /**
     * The full great window of the hit object.
     */
    greatWindow: Double
) : DifficultyHitObject(obj, lastObj, lastLastObj, clockRate, difficultyHitObjects, index, greatWindow) {
    override val mode = GameMode.Standard

    override val scalingFactor: Float
        get() {
            // We will scale distances by this factor, so we can assume a uniform CircleSize among beatmaps.
            val radius = obj.difficultyRadius.toFloat()
            var scalingFactor = NORMALIZED_RADIUS / radius

            // High circle size (small CS) bonus
            if (radius < 30) {
                scalingFactor *= 1 + min(30 - radius, 5.0f) / 50
            }

            return scalingFactor
        }

}