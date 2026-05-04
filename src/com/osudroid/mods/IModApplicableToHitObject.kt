package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.hitobjects.HitObject
import kotlinx.coroutines.CoroutineScope

/**
 * An interface for [Mod]s that can be applied to [HitObject]s.
 */
interface IModApplicableToHitObject {
    /**
     * Applies this [IModApplicableToHitObject] to a [HitObject].
     *
     * @param mode The [GameMode] to apply for.
     * @param hitObject The [HitObject] to apply to.
     * @param adjustmentMods [Mod]s that apply [IModFacilitatesAdjustment].
     * @param scope The [CoroutineScope] to use for the operation.
     */
    fun applyToHitObject(mode: GameMode, hitObject: HitObject, adjustmentMods: Iterable<IModFacilitatesAdjustment>, scope: CoroutineScope? = null)
}