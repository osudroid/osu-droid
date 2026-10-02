package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Sudden Death mod.
 */
class ModSuddenDeath : Mod() {
    override val name = "Sudden Death"
    override val acronym = "SD"
    override val description = "Miss and fail."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(
            ModNoFail::class,
            ModPerfect::class,
            ModAutoplay::class
        ) && super.isCompatibleWith(other)
}