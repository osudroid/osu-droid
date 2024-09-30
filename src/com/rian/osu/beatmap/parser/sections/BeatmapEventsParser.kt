package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.timings.BreakPeriod
import ru.nsu.ccfit.zuev.osu.RGBColor
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A parser for parsing a beatmap's events section.
 */
object BeatmapEventsParser : BeatmapSectionParser() {
    private val splitRegex = "\\s*,\\s*".toRegex()

    override fun parse(beatmap: Beatmap, line: String, scope: CoroutineScope?) = line
        .split(splitRegex)
        .dropLastWhile {
            scope?.ensureActive()
            it.isEmpty()
        }
        .let {
            if (it.size >= 3) {
                if (line.startsWith("0,0")) {
                    beatmap.events.backgroundFilename = cleanFilename(it[2])
                }

                if (line.startsWith("2") || line.startsWith("Break")) {
                    val start = beatmap.getOffsetTime(parseInt(it[1]))
                    val end = max(start, beatmap.getOffsetTime(parseInt(it[2])))

                    beatmap.events.breaks.add(BreakPeriod(start.toFloat(), end.toFloat()))
                }

                if (line.startsWith("1") || line.startsWith("Video")) {
                    beatmap.events.videoStartTime = parseInt(it[1])
                    beatmap.events.videoFilename = cleanFilename(it[2])
                }
            }
    
            if (it.size >= 5 && line.startsWith("3")) {
                beatmap.events.backgroundColor = RGBColor(
                    parseInt(it[2]).toFloat(),
                    parseInt(it[3]).toFloat(),
                    parseInt(it[4]).toFloat()
                )
            }
        }

    private fun cleanFilename(path: String) = path.replace("\\\\", "\\").trim { it == '"' }
}
