package com.osudroid.mods

import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Traceable mod.
 */
class ModTraceable : ModWithVisibilityAdjustment() {
    override val name = "Traceable"
    override val acronym = "TC"
    override val description = "Put your faith in the approach circles..."
    override val type = ModType.DifficultyIncrease

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(ModHidden::class) && super.isCompatibleWith(other)

    override fun isFirstAdjustableObject(hitObject: HitObject) = hitObject !is Spinner
}