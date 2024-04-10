package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.BeatmapCountdown
import com.rian.osu.beatmap.constants.SampleBank

/**
 * A parser for parsing a beatmap's general section.
 */
object BeatmapGeneralParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String) = splitProperty(line).let {
        when (it[0]) {
            "AudioFilename" -> beatmap.general.audioFilename = it[1]
            "AudioLeadIn" -> beatmap.general.audioLeadIn = parseInt(it[1])
            "PreviewTime" -> beatmap.general.previewTime = beatmap.getOffsetTime(parseInt(it[1]))
            "Countdown" -> beatmap.general.countdown = BeatmapCountdown.parse(it[1])
            "SampleSet" -> beatmap.general.sampleBank = SampleBank.parse(it[1])
            "SampleVolume" -> beatmap.general.sampleVolume = parseInt(it[1])
            "StackLeniency" -> beatmap.general.stackLeniency = parseFloat(it[1])
            "LetterboxInBreaks" -> beatmap.general.letterboxInBreaks = it[1] == "1"
            "Mode" -> beatmap.general.mode = parseInt(it[1])
        }
    }
}
