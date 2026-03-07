package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.beatmap.hitobject.sliderobject.SliderRepeat
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Freeze Frame mod.
 */
class ModFreezeFrame : Mod(), IModApplicableToBeatmap {
    override val name = "Freeze Frame"
    override val acronym = "FR"
    override val description = "Burn the notes into your memory."
    override val type = ModType.Fun
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModApproachDifferent::class, ModHidden::class)

    private var lastNewComboTime = 0.0

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        lastNewComboTime = 0.0

        for (obj in beatmap.hitObjects.objects) {
            scope?.ensureActive()

            if (obj.isNewCombo) {
                lastNewComboTime = obj.startTime
            }

            applyFadeInAdjustment(obj)
        }
    }

    private fun applyFadeInAdjustment(hitObject: HitObject) {
        if (hitObject !is Spinner) {
            hitObject.timePreempt += hitObject.startTime - lastNewComboTime
        }

        if (hitObject is Slider) {
            hitObject.nestedHitObjects.forEach {
                when (it) {
                    is SliderTick -> {
                        // Freezing slider ticks doesn't play well with snaking sliders.
                    }

                    is SliderRepeat -> {
                        // Any more than 2 repeats will not layer correctly if its preempt is changed.
                        if (it.spanIndex > 1) {
                            return@forEach
                        }

                        applyFadeInAdjustment(it)
                    }

                    else -> applyFadeInAdjustment(it)
                }
            }
        }
    }
}