package com.rian.osu.mods

/**
 * Represents the Relax mod.
 */
class ModRelax : Mod() {
    override val droidString = "x"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModAuto::class, ModNoFail::class, ModAutopilot::class
    )
}