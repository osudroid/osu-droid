package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap

/**
 * A parser for parsing a beatmap's difficulty section.
 */
object BeatmapDifficultyParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String) = splitProperty(line).let {
        when (it.first) {
            "CircleSize" -> beatmap.difficulty.cs = parseFloat(it.second)
            "OverallDifficulty" -> beatmap.difficulty.od = parseFloat(it.second)
            "ApproachRate" -> beatmap.difficulty.ar = parseFloat(it.second)
            "HPDrainRate" -> beatmap.difficulty.hp = parseFloat(it.second)
            "SliderMultiplier" -> beatmap.difficulty.sliderMultiplier = parseDouble(it.second).coerceIn(0.4, 3.6)
            "SliderTickRate" -> beatmap.difficulty.sliderTickRate = parseDouble(it.second).coerceIn(0.5, 8.0)
        }
    }
}
