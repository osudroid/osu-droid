package com.osudroid.beatmaps.parser.sections

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.constants.BeatmapCountdown
import com.osudroid.beatmaps.constants.SampleBank
import kotlinx.coroutines.CoroutineScope

/**
 * A parser for parsing a beatmap's general section.
 */
object BeatmapGeneralParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope?) = splitProperty(line, scope)?.let {
        when (it.first) {
            "AudioFilename" -> beatmap.general.audioFilename = it.second
            "AudioLeadIn" -> beatmap.general.audioLeadIn = parseInt(it.second)
            "PreviewTime" -> beatmap.general.previewTime = beatmap.getOffsetTime(parseInt(it.second))
            "Countdown" -> beatmap.general.countdown = BeatmapCountdown.parse(it.second)
            "SampleSet" -> beatmap.general.sampleBank = SampleBank.parse(it.second)
            "SampleVolume" -> beatmap.general.sampleVolume = parseInt(it.second)
            "StackLeniency" -> beatmap.general.stackLeniency = parseFloat(it.second)
            "LetterboxInBreaks" -> beatmap.general.letterboxInBreaks = it.second == "1"
            "EpilepsyWarning" -> beatmap.general.epilepsyWarning = it.second == "1"
            "Mode" -> beatmap.general.mode = parseInt(it.second)
            "SamplesMatchPlaybackRate" -> beatmap.general.samplesMatchPlaybackRate = it.second == "1"
        }
    } ?: throw UnsupportedOperationException("Malformed general property: $line")
}
