package com.rian.osu.mods

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Spinner

/**
 * Represents the Traceable mod.
 */
class ModTraceable : ModWithVisibilityAdjustment() {
    override val name = "Traceable"
    override val acronym = "TC"
    override val description = "Put your faith in the approach circles..."
    override val type = ModType.DifficultyIncrease
    override val scoreMultiplier = 1.06f
    override val incompatibleMods = super.incompatibleMods + ModHidden::class

    override fun isFirstAdjustableObject(hitObject: HitObject) = hitObject !is Spinner
    override fun deepCopy() = ModTraceable()
}