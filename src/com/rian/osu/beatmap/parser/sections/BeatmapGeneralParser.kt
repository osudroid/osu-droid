package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.BeatmapCountdown
import com.rian.osu.beatmap.constants.SampleBank

/**
 * A parser for parsing a beatmap's general section.
 */
object BeatmapGeneralParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String) = splitProperty(line)?.let {
        when (it.first) {
            "AudioFilename" -> beatmap.general.audioFilename = it.second
            "AudioLeadIn" -> beatmap.general.audioLeadIn = parseInt(it.second)
            "PreviewTime" -> beatmap.general.previewTime = beatmap.getOffsetTime(parseInt(it.second))
            "Countdown" -> beatmap.general.countdown = BeatmapCountdown.parse(it.second)
            "SampleSet" -> beatmap.general.sampleBank = SampleBank.parse(it.second)
            "SampleVolume" -> beatmap.general.sampleVolume = parseInt(it.second)
            "StackLeniency" -> beatmap.general.stackLeniency = parseFloat(it.second)
            "LetterboxInBreaks" -> beatmap.general.letterboxInBreaks = it.second == "1"
            "Mode" -> beatmap.general.mode = parseInt(it.second)
        }
    } ?: throw UnsupportedOperationException("Malformed general property: $line")
}
