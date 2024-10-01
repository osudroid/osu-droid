package com.rian.osu.mods

/**
 * Represents the Sudden Death mod.
 */
class ModSuddenDeath : Mod() {
    override val droidString = "u"
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModPerfect::class, ModAuto::class
    )
}