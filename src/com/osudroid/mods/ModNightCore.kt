package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Night Core mod.
 */
open class ModNightCore : ModRateAdjust() {

    override var trackRateMultiplier = 1.5f

    override val name = "Nightcore"
    override val acronym = "NC"
    override val description = "Uguuuuuuuu..."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(ModDoubleTime::class, ModHalfTime::class) && super.isCompatibleWith(other)
}