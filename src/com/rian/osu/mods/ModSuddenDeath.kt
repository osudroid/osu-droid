package com.rian.osu.mods

/**
 * Represents the Sudden Death mod.
 */
class ModSuddenDeath : Mod() {
    override val name = "Sudden Death"
    override val acronym = "SD"
    override val description = "Miss and fail."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModPerfect::class, ModAutoplay::class
    )
}