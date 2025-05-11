package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import kotlinx.coroutines.CoroutineScope

/**
 * Represents the Freeze Frame mod.
 */
class ModFreezeFrame : Mod(), IModApplicableToBeatmap {
    override val name = "Freeze Frame"
    override val acronym = "FR"
    override val description = "Burn the notes into your memory."
    override val type = ModType.Fun

    private var lastNewComboTime = 0.0

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        lastNewComboTime = 0.0

        for (obj in beatmap.hitObjects.objects) {
            if (obj.isNewCombo) {
                lastNewComboTime = obj.startTime
            }

            applyFadeInAdjustment(obj)
        }
    }

    private fun applyFadeInAdjustment(hitObject: HitObject) {
        hitObject.timePreempt += hitObject.startTime - lastNewComboTime

        if (hitObject is Slider) {
            // Freezing slider ticks doesn't play well with snaking sliders, and slider repeats will not layer
            // correctly if its preempt is changed.
            applyFadeInAdjustment(hitObject.head)
            applyFadeInAdjustment(hitObject.tail)
        }
    }

    override fun deepCopy() = ModFreezeFrame()
}