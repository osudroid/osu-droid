package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.RGBColor

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
        fun getColorFor(beatDivisor: Int): RGBColor = beatDivisorColors[beatDivisor] ?: defaultDivisorColor

        private val beatDivisorColors = mutableMapOf<Int, RGBColor>(
            1 to RGBColor(1f, 1f, 1f),
            2 to RGBColor.hex2Rgb("#ED1121"),
            3 to RGBColor.hex2Rgb("#8866EE"),
            4 to RGBColor.hex2Rgb("#66CCFF"),
            6 to RGBColor.hex2Rgb("#EEAA00"),
            8 to RGBColor.hex2Rgb("#FFCC22"),
            12 to RGBColor.hex2Rgb("#CC6600"),
            16 to RGBColor.hex2Rgb("#441188")
        )

        private val defaultDivisorColor = RGBColor.hex2Rgb("#ff0000")
    }
}