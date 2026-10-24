package com.osudroid.mods

import com.osudroid.utils.isInstanceOfAny

/**
 * Represents the Relax mod.
 */
class ModRelax : Mod() {
    override val name = "Relax"
    override val acronym = "RX"
    override val description = "You don't need to tap. Give your tapping fingers a break from the heat of things."
    override val type = ModType.Automation

    override fun isCompatibleWith(other: Mod) =
        !other.isInstanceOfAny(
            ModAutoplay::class,
            ModNoFail::class,
            ModAutopilot::class
        ) && super.isCompatibleWith(other)

    companion object {
        /**
         * How early before a hit object's start time (in milliseconds) a cursor flowing over it can trigger a hit.
         */
        const val RELAX_LENIENCY = 12.0
    }
}