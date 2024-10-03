package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Hidden mod.
 */
class ModHidden : Mod(), IModUserSelectable, IModApplicableToBeatmap {
    override val droidString = "h"
    override val acronym = "HD"
    override val textureNameSuffix = "hidden"
    override val ranked = true

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun applyToBeatmap(beatmap: Beatmap) {
        fun applyFadeInAdjustment(hitObject: HitObject) {
            hitObject.timeFadeIn = hitObject.timePreempt * FADE_IN_DURATION_MULTIPLIER

            if (hitObject is Slider) {
                hitObject.nestedHitObjects.forEach { applyFadeInAdjustment(it) }
            }
        }

        beatmap.hitObjects.objects.forEach { applyFadeInAdjustment(it) }
    }

    override fun equals(other: Any?) = other === this || other is ModHidden
    override fun hashCode() = super.hashCode()

    companion object {
        const val FADE_IN_DURATION_MULTIPLIER = 0.4
        const val FADE_OUT_DURATION_MULTIPLIER = 0.3
    }
}