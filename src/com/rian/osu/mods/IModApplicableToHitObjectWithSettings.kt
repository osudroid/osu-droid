package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject

/**
 * An interface for [Mod]s that can be applied to [HitObject]s.
 *
 * This is used in place of [IModApplicableToHitObject] to make adjustments that
 * correlates directly to other applied [Mod]s and settings.
 *
 * [Mod]s marked by this interface will have their adjustments applied after
 * [IModApplicableToHitObject] [Mod]s have been applied.
 */
interface IModApplicableToHitObjectWithSettings {
    /**
     * Applies this [IModApplicableToHitObjectWithSettings] to a [HitObject].
     *
     * This is typically called post beatmap conversion.
     *
     * @param mode The [GameMode] to apply for.
     * @param hitObject The [HitObject] to mutate.
     * @param mods The [Mod]s that are used.
     * @param customSpeedMultiplier The custom speed multiplier that is used.
     * @param oldStatistics Whether to enforce old statistics. Some [Mod]s behave differently with this flag.
     * For example, [ModNightCore] will apply a 1.39 rate multiplier instead of 1.5 when this is `true`.
     * **Never set this flag to `true` unless you know what you are doing.**
     */
    fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: Iterable<Mod>, customSpeedMultiplier: Float, oldStatistics: Boolean)
}