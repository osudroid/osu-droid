package com.rian.osu.mods

/**
 * Represents the Autopilot mod.
 */
class ModAutopilot : Mod() {
    override val droidString = "p"
    override val acronym = "AP"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAuto::class, ModNoFail::class
    )
}