package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : ModRateAdjust() {

    override var trackRateMultiplier = 0.75f

    override val name = "Half Time"
    override val acronym = "HT"
    override val description = "Less zoom..."
    override val type = ModType.DifficultyReduction
    override val isRanked = true

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(ModDoubleTime::class, ModNightCore::class) && super.isCompatibleWith(other)
}