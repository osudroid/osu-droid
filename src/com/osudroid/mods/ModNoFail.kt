package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the No Fail mod.
 */
class ModNoFail : Mod() {
    override val name = "No Fail"
    override val acronym = "NF"
    override val description = "You can't fail, no matter what."
    override val type = ModType.DifficultyReduction
    override val isRanked = true

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(
            ModPerfect::class,
            ModSuddenDeath::class,
            ModAutopilot::class,
            ModRelax::class
        ) && super.isCompatibleWith(other)
}