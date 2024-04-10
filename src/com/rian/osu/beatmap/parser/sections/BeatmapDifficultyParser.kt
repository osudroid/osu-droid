package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap

/**
 * A parser for parsing a beatmap's difficulty section.
 */
object BeatmapDifficultyParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String) = splitProperty(line).let {
        when (it[0]) {
            "CircleSize" -> beatmap.difficulty.cs = parseFloat(it[1])
            "OverallDifficulty" -> beatmap.difficulty.od = parseFloat(it[1])
            "ApproachRate" -> beatmap.difficulty.ar = parseFloat(it[1])
            "HPDrainRate" -> beatmap.difficulty.hp = parseFloat(it[1])
            "SliderMultiplier" -> beatmap.difficulty.sliderMultiplier = parseDouble(it[1]).coerceIn(0.4, 3.6)
            "SliderTickRate" -> beatmap.difficulty.sliderTickRate = parseDouble(it[1]).coerceIn(0.5, 8.0)
        }
    }
}
