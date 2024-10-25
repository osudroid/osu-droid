package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.timings.DifficultyControlPoint
import com.rian.osu.beatmap.timings.EffectControlPoint
import com.rian.osu.beatmap.timings.SampleControlPoint
import com.rian.osu.beatmap.timings.TimingControlPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A parser for parsing a beatmap's timing points section.
 */
object BeatmapControlPointsParser : BeatmapSectionParser() {
    override fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope?) = line
        .split(COMMA_PROPERTY_REGEX)
        .dropLastWhile {
            scope?.ensureActive()
            it.isEmpty()
        }
        .let {
            if (it.size < 2) {
                throw UnsupportedOperationException("Malformed timing point")
            }

            val time = beatmap.getOffsetTime(parseDouble(it[0].trim { s -> s <= ' ' }))

            // msPerBeat is allowed to be NaN to handle an edge case in which some
            // beatmaps use NaN slider velocity to disable slider tick generation.
            val msPerBeat = parseDouble(it[1].trim { s -> s <= ' ' }, allowNaN = true)

            val timeSignature = it.getOrNull(2)?.let { s -> parseInt(s) } ?: 4
            if (timeSignature < 1) {
                throw UnsupportedOperationException("The numerator of a time signature must be positive")
            }

            var sampleSet = it.getOrNull(3)?.let { s -> SampleBank.parse(parseInt(s)) } ?: beatmap.general.sampleBank
            val customSampleBank = it.getOrNull(4)?.let { s -> parseInt(s) } ?: 0
            val sampleVolume = it.getOrNull(5)?.let { s -> parseInt(s) } ?: beatmap.general.sampleVolume

            val timingChange = it.getOrNull(6)?.let { s -> s == "1" } ?: true
            val isKiai = it.getOrNull(7)?.let { s -> parseInt(s) and 1 != 0 } ?: false

            if (sampleSet == SampleBank.None) {
                sampleSet = SampleBank.Normal
            }

            scope?.ensureActive()

            beatmap.controlPoints.apply {
                if (timingChange) {
                    if (msPerBeat.isNaN()) {
                        throw UnsupportedOperationException("Beat length cannot be NaN in a timing control point")
                    }

                    timing.add(TimingControlPoint(time, msPerBeat, timeSignature))
                }

                difficulty.add(
                    DifficultyControlPoint(
                        time,  // If msPerBeat is NaN, speedMultiplier should still be 1 because all comparisons against NaN are false.
                        if (msPerBeat < 0) (100 / -msPerBeat).coerceIn(0.1, 10.0) else 1.0,
                        !msPerBeat.isNaN()
                    )
                )

                effect.add(EffectControlPoint(time, isKiai))

                sample.add(SampleControlPoint(time, sampleSet, sampleVolume, customSampleBank))
            }

            Unit
        }


}
