package com.osudroid.mods

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Spinner

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
}