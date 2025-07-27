package com.rian.osu.mods

import com.reco1l.framework.*

/**
 * Represents the Synesthesia mod.
 */
class ModSynesthesia : Mod() {
    override val name = "Synesthesia"
    override val acronym = "SY"
    override val description = "Colors hit objects based on the rhythm."
    override val type = ModType.Fun
    override val scoreMultiplier = 0.8f

    override fun deepCopy() = ModSynesthesia()

    companion object {
        /**
         * Retrieves the appropriate color for a beat divisor.
         *
         * @param beatDivisor The beat divisor.
         * @return The applicable color for [beatDivisor].
         */
        @JvmStatic
        fun getColorFor(beatDivisor: Int): Color4 = beatDivisorColors[beatDivisor] ?: defaultDivisorColor

        private val beatDivisorColors = mutableMapOf(
            1 to Color4(1f, 1f, 1f),
            2 to Color4("#ED1121"),
            3 to Color4("#8866EE"),
            4 to Color4("#66CCFF"),
            6 to Color4("#EEAA00"),
            8 to Color4("#FFCC22"),
            12 to Color4("#CC6600"),
            16 to Color4("#441188")
        )

        private val defaultDivisorColor = Color4("#ff0000")
    }
}