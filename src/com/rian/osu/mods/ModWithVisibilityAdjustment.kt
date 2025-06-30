package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A [Mod] which applies visibility adjustments to [HitObject]s with an optional increased visibility adjustment
 * depending on the user's "increase first object visibility" setting.
 */
sealed class ModWithVisibilityAdjustment : Mod(), IModApplicableToBeatmap {
    /**
     * The first adjustable [HitObject].
     */
    var firstObject: HitObject? = null
        private set

    /**
     * Checks whether the provided [HitObject] should be considered as the "first" adjustable object. Can be used to
     * skip spinners, for instance.
     *
     * @param hitObject The [HitObject] to check.
     * @return `true` if the [HitObject] is the first adjustable object, `false` otherwise.
     */
    protected open fun isFirstAdjustableObject(hitObject: HitObject) = true

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        firstObject = getFirstAdjustableObject(beatmap.hitObjects, scope)
    }

    private fun getFirstAdjustableObject(hitObjects: Iterable<HitObject>, scope: CoroutineScope?): HitObject? {
        for (hitObject in hitObjects) {
            scope?.ensureActive()

            if (isFirstAdjustableObject(hitObject)) {
                return hitObject
            }

            if (hitObject is Slider) {
                val nestedResult = getFirstAdjustableObject(hitObject.nestedHitObjects, scope)

                if (nestedResult != null) {
                    return nestedResult
                }
            }
        }

        return null
    }
}