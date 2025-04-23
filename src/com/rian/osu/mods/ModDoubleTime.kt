package com.rian.osu.mods

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : ModRateAdjust() {

    override var trackRateMultiplier = 1.5f

    override val name = "Double Time"
    override val acronym = "DT"
    override val description = "Zoooooooooom..."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModNightCore::class, ModHalfTime::class)

    override fun equals(other: Any?) = other === this || other is ModDoubleTime
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModDoubleTime()
}