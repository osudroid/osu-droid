package com.osudroid.mods

import com.osudroid.mods.settings.BooleanModSetting

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

    /**
     * When enabled, the cursor sweeps arcing ("dance") paths between hit objects
     * instead of moving in straight lines.
     */
    @get:JvmName("isDanceCursor")
    var danceCursor by BooleanModSetting(
        name = "Dancing cursor",
        key = "danceCursor",
        defaultValue = false
    )

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(
            ModRelax::class,
            ModAutopilot::class,
            ModPerfect::class,
            ModSuddenDeath::class
        ) && super.isCompatibleWith(other)
}
