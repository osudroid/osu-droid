package com.rian.osu.mods

import com.reco1l.framework.*
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Synesthesia mod.
 */
class ModSynesthesia : Mod() {
    override val name = "Synesthesia"
    override val acronym = "SY"
    override val description = "Colors hit objects based on the rhythm."
    override val type = ModType.Fun

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.8f
    override fun deepCopy() = ModSynesthesia()

    companion object {
        /**
         * Retrieves the appropriate color for a beat divisor.
         *
         * @param beatDivisor The beat divisor.
         * @return The applicable color for [beatDivisor].
         */
        @JvmStatic
        fun getColorFor(beatDivisor: Int): ColorARGB = beatDivisorColors[beatDivisor] ?: defaultDivisorColor

        private val beatDivisorColors = mutableMapOf(
            1 to ColorARGB(1f, 1f, 1f),
            2 to ColorARGB("#ED1121"),
            3 to ColorARGB("#8866EE"),
            4 to ColorARGB("#66CCFF"),
            6 to ColorARGB("#EEAA00"),
            8 to ColorARGB("#FFCC22"),
            12 to ColorARGB("#CC6600"),
            16 to ColorARGB("#441188")
        )

        private val defaultDivisorColor = ColorARGB("#ff0000")
    }
}