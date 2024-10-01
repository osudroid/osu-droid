package com.rian.osu.mods

/**
 * Represents the Auto mod.
 */
class ModAuto : Mod() {
    override val droidString = "a"
    override val acronym = "AT"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutopilot::class, ModPerfect::class, ModSuddenDeath::class
    )
}
