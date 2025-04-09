package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Precise mod.
 */
class ModPrecise : Mod(), IModApplicableToHitObject {
    override val name = "Precise"
    override val acronym = "PR"
    override val type = ModType.DifficultyIncrease
    override val textureNameSuffix = "precise"
    override val isRanked = true

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1.06f

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject) {
        if (mode == GameMode.Standard) {
            return
        }

        // For sliders, the hit window is enforced in the head - everything else is an instant hit or miss.
        val obj = if (hitObject is Slider) hitObject.head else hitObject

        obj.hitWindow = PreciseDroidHitWindow(obj.hitWindow?.overallDifficulty)
    }

    override fun equals(other: Any?) = other === this || other is ModPrecise
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModPrecise()
}