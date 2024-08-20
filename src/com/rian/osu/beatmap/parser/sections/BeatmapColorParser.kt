package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.ComboColor
import ru.nsu.ccfit.zuev.osu.RGBColor

/**
 * A parser for parsing a beatmap's colors section.
 */
object BeatmapColorParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String) = splitProperty(line)?.let { p ->
        val s = p.second.split(COMMA_PROPERTY_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (s.size != 3 && s.size != 4) {
            throw UnsupportedOperationException("Color specified in incorrect format (should be R,G,B or R,G,B,A)")
        }

        val color = RGBColor(
            parseInt(s[0]).toFloat(),
            parseInt(s[1]).toFloat(),
            parseInt(s[2]).toFloat()
        )

        if (p.first.startsWith("Combo")) {
            val index = p.first.substring(5).toIntOrNull() ?: (beatmap.colors.comboColors.size + 1)

            beatmap.colors.comboColors.apply {
                add(ComboColor(index, color))
                
                sortBy { it.index }
            }
        }

        if (p.first.startsWith("SliderBorder")) {
            beatmap.colors.sliderBorderColor = color
        }
    } ?: throw UnsupportedOperationException("Malformed color property: $line")
}
