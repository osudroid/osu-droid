package com.rian.osu.mods

/**
 * Represents the Half Time mod.
 */
class ModHalfTime : ModRateAdjust() {

    override var trackRateMultiplier = 0.75f

    override val name = "Half Time"
    override val acronym = "HT"
    override val type = ModType.DifficultyReduction
    override val textureNameSuffix = "halftime"
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModDoubleTime::class, ModNightCore::class)

    override fun equals(other: Any?) = other === this || other is ModHalfTime
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModHalfTime()
}