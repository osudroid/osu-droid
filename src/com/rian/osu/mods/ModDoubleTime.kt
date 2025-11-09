package com.rian.osu.mods

/**
 * Represents the Double Time mod.
 */
class ModDoubleTime : ModRateAdjust() {
    override val name = "Double Time"
    override val acronym = "DT"
    override val description = "Zoooooooooom..."
    override val type = ModType.DifficultyIncrease
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModNightCore::class, ModHalfTime::class)

    override var trackRateMultiplier = 1.5f
}