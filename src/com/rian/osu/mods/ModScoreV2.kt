package com.rian.osu.mods

/**
 * Represents the ScoreV2 mod.
 */
class ModScoreV2 : Mod() {
    override val name = "Score V2"
    override val acronym = "V2"
    override val description = "A different scoring mode from what you have known."
    override val type = ModType.Conversion
    override val isValidForMultiplayer = false

    override fun deepCopy() = ModScoreV2()
}