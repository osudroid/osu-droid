package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider

/**
 * Represents the Hidden mod.
 */
class ModHidden : Mod(), IModApplicableToBeatmap {
    override val droidString = "h"

    override fun applyToBeatmap(beatmap: Beatmap) {
        fun applyFadeInAdjustment(hitObject: HitObject) {
            hitObject.timeFadeIn = hitObject.timePreempt * FADE_IN_DURATION_MULTIPLIER

            if (hitObject is Slider) {
                hitObject.nestedHitObjects.forEach { applyFadeInAdjustment(it) }
            }
        }

        beatmap.hitObjects.objects.forEach { applyFadeInAdjustment(it) }
    }

    companion object {
        const val FADE_IN_DURATION_MULTIPLIER = 0.4
        const val FADE_OUT_DURATION_MULTIPLIER = 0.3
    }
}