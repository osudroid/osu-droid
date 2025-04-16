package com.rian.osu.mods

/**
 * Represents the Sudden Death mod.
 */
class ModSuddenDeath : Mod() {
    override val name = "Sudden Death"
    override val acronym = "SD"
    override val description = "Miss and fail."
    override val type = ModType.DifficultyIncrease
    override val textureNameSuffix = "suddendeath"
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModPerfect::class, ModAuto::class
    )

    override fun equals(other: Any?) = other === this || other is ModSuddenDeath
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModSuddenDeath()
}