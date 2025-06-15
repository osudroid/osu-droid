package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject

/**
 * An interface for [Mod]s that can be applied to [HitObject]s.
 *
 * This is used in place of [IModApplicableToHitObject] to make adjustments that
 * correlates directly to other applied [Mod]s.
 *
 * [Mod]s marked by this interface will have their adjustments applied after
 * [IModApplicableToHitObject] [Mod]s have been applied.
 */
interface IModApplicableToHitObjectWithMods {
    /**
     * Applies this [IModApplicableToHitObjectWithMods] to a [HitObject].
     *
     * This is typically called post beatmap conversion.
     *
     * @param mode The [GameMode] to apply for.
     * @param hitObject The [HitObject] to mutate.
     * @param mods The [Mod]s that are used.
     */
    fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: Iterable<Mod>)
}