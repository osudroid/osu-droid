package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import kotlinx.coroutines.CoroutineScope

/**
 * A parser for parsing a beatmap's difficulty section.
 */
object BeatmapDifficultyParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope?) = splitProperty(line, scope)?.let {
        when (it.first) {
            "CircleSize" -> {
                val value = parseFloat(it.second)

                beatmap.difficulty.difficultyCS = value
                beatmap.difficulty.gameplayCS = value
            }

            "OverallDifficulty" -> beatmap.difficulty.od = parseFloat(it.second)
            "ApproachRate" -> beatmap.difficulty.ar = parseFloat(it.second)
            "HPDrainRate" -> beatmap.difficulty.hp = parseFloat(it.second)
            "SliderMultiplier" -> beatmap.difficulty.sliderMultiplier = parseDouble(it.second).coerceIn(0.4, 3.6)
            "SliderTickRate" -> beatmap.difficulty.sliderTickRate = parseDouble(it.second).coerceIn(0.5, 8.0)
        }
    } ?: throw UnsupportedOperationException("Malformed difficulty property: $line")
}
