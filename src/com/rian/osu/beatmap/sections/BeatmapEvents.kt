package com.rian.osu.beatmap.sections

import ru.nsu.ccfit.zuev.osu.RGBColor
import com.rian.osu.beatmap.timings.BreakPeriod

/**
 * Contains beatmap events.
 */
class BeatmapEvents {
    /**
     * The file name of this beatmap's background.
     */
    @JvmField
    var backgroundFilename: String? = null

    /**
     * The file name of this beatmap's background video.
     */
    @JvmField
    var videoFilename: String? = null

    /**
     * The beatmap's background video start time in milliseconds.
     */
    @JvmField
    var videoStartTime = 0

    /**
     * The breaks this beatmap has.
     */
    @JvmField
    var breaks = mutableListOf<BreakPeriod>()

    /**
     * The background color of this beatmap.
     */
    @JvmField
    var backgroundColor: RGBColor? = null
}
