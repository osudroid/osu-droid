package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Autoplay mod.
 */
class ModAutoplay : Mod() {
    override val name = "Autoplay"
    override val acronym = "AT"
    override val description = "Watch a perfect automated play through the song."
    override val type = ModType.Automation
    override val isValidForMultiplayer = false
    override val isValidForMultiplayerAsFreeMod = false

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(
            ModRelax::class,
            ModAutopilot::class,
            ModPerfect::class,
            ModSuddenDeath::class
        ) && super.isCompatibleWith(other)
}
