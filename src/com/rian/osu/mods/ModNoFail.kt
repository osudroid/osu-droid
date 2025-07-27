package com.rian.osu.mods

/**
 * Represents the No Fail mod.
 */
class ModNoFail : Mod() {
    override val name = "No Fail"
    override val acronym = "NF"
    override val description = "You can't fail, no matter what."
    override val type = ModType.DifficultyReduction
    override val isRanked = true
    override val isUserPlayable = false
    override val scoreMultiplier = 0.5f

    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModPerfect::class, ModSuddenDeath::class, ModAutopilot::class, ModRelax::class
    )

    override fun deepCopy() = ModNoFail()
}