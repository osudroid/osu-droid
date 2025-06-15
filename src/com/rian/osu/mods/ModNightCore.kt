package com.rian.osu.mods

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
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModDoubleTime::class, ModHalfTime::class)

    override fun deepCopy() = ModNightCore()
}