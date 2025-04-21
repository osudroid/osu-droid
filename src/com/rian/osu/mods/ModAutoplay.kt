package com.rian.osu.mods

/**
 * Represents the Autoplay mod.
 */
class ModAutoplay : Mod() {
    override val name = "Autoplay"
    override val acronym = "AT"
    override val description = "Watch a perfect automated play through the song."
    override val type = ModType.Automation
    override val textureNameSuffix = "autoplay"
    override val isValidForMultiplayer = false
    override val isValidForMultiplayerAsFreeMod = false
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModRelax::class, ModAutopilot::class, ModPerfect::class, ModSuddenDeath::class
    )

    override fun equals(other: Any?) = other === this || other is ModAutoplay
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModAutoplay()
}
