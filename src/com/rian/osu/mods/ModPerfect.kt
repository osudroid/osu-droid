package com.rian.osu.mods

/**
 * Represents the Perfect mod.
 */
class ModPerfect : Mod() {
    override val name = "Perfect"
    override val acronym = "PF"
    override val description = "SS or quit."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModSuddenDeath::class, ModAutoplay::class
    )

    override fun deepCopy() = ModPerfect()
}