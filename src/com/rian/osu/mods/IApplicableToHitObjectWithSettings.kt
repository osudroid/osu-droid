package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject

/**
 * An interface for [Mod]s that can be applied to [HitObject]s based on other applied [Mod]s and settings.
 *
 * This should not be used together with [IApplicableToHitObject].
 */
interface IApplicableToHitObjectWithSettings {
    /**
     * Applies this [IApplicableToHitObjectWithSettings] to a [HitObject].
     *
     * This is typically called post beatmap conversion.
     *
     * @param mode The [GameMode] to apply for.
     * @param hitObject The [HitObject] to mutate.
     * @param mods The [Mod]s that are used.
     * @param customSpeedMultiplier The custom speed multiplier that is used.
     */
    fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: List<Mod>, customSpeedMultiplier: Float)
}