package com.rian.osu.mods

import com.osudroid.ui.OsuColors
import com.osudroid.ui.toRGBColor
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

    override fun equals(other: Any?) = other === this || other is ModSynesthesia
    override fun hashCode() = super.hashCode()
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
            2 to OsuColors.red.toRGBColor(),
            3 to OsuColors.purple.toRGBColor(),
            4 to OsuColors.blue.toRGBColor(),
            6 to OsuColors.yellowDark.toRGBColor(),
            8 to OsuColors.yellow.toRGBColor(),
            12 to OsuColors.yellowDarker.toRGBColor(),
            16 to OsuColors.purpleDark.toRGBColor(),
        )

        private val defaultDivisorColor = RGBColor.hex2Rgb("#ff0000")
    }
}