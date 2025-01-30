package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider

/**
 * Represents the Precise mod.
 */
class ModPrecise : Mod(), IModApplicableToHitObject {
    override val droidString = "s"

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject) {
        if (mode == GameMode.Standard) {
            return
        }

        // For sliders, the hit window is enforced in the head - everything else is an instant hit or miss.
        val obj = if (hitObject is Slider) hitObject.head else hitObject

        obj.hitWindow = PreciseDroidHitWindow(obj.hitWindow?.overallDifficulty)
    }
}