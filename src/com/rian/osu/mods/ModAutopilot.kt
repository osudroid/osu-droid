package com.rian.osu.mods

/**
 * Represents the Autopilot mod.
 */
class ModAutopilot : Mod() {
    override val name = "Autopilot"
    override val acronym = "AP"
    override val description = "Automatic cursor movement - just follow the rhythm."
    override val type = ModType.Automation
    override val iconTextureNameSuffix = "relax2"
    override val scoreMultiplier = 1e-3f
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutoplay::class, ModNoFail::class
    )

    override fun deepCopy() = ModAutopilot()
}