package com.rian.osu.mods

/**
 * Represents the Perfect mod.
 */
class ModPerfect : Mod() {
    override val droidString = "f"
    override val acronym = "PF"
    override val ranked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModNoFail::class, ModSuddenDeath::class, ModAuto::class
    )
}