package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Perfect mod.
 */
class ModPerfect : Mod() {
    override val name = "Perfect"
    override val acronym = "PF"
    override val description = "SS or quit."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(
            ModNoFail::class,
            ModSuddenDeath::class,
            ModAutoplay::class
        ) && super.isCompatibleWith(other)
}