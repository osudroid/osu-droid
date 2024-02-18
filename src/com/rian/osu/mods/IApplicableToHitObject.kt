package com.rian.osu.mods

import com.rian.osu.beatmap.hitobject.HitObject

/**
 * An interface for [Mod]s that can be applied to [HitObject]s.
 */
interface IApplicableToHitObject {
    /**
     * Applies this [IApplicableToHitObject] to a [HitObject].
     *
     * @param hitObject The [HitObject] to apply to.
     */
    fun applyToHitObject(hitObject: HitObject)
}