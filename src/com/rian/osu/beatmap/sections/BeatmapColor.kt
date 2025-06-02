package com.rian.osu.beatmap.sections

import com.reco1l.framework.*
import com.rian.osu.beatmap.ComboColor

/**
 * Contains information about combo and skin colors of a beatmap.
 */
class BeatmapColor {
    /**
     * The combo colors of this beatmap.
     */
    @JvmField
    val comboColors = mutableListOf<ComboColor>()

    /**
     * The color of the slider border.
     */
    var sliderBorderColor: ColorARGB? = null
}
