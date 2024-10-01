package com.rian.osu.mods

/**
 * Represents the No Fail mod.
 */
class ModNoFail : Mod() {
    override val droidString = "n"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModPerfect::class, ModSuddenDeath::class, ModAutopilot::class, ModRelax::class
    )
}