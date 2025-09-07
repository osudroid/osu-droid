package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import kotlin.math.max

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
    index: Int
) : DifficultyHitObject(obj, lastObj, clockRate, difficultyHitObjects, index) {
    override val mode = GameMode.Standard
    override val smallCircleBonus = max(1.0, 1 + (30 - obj.difficultyRadius) / 40)
}