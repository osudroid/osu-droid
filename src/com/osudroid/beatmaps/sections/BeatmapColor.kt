package com.osudroid.beatmaps.sections

import com.osudroid.beatmaps.ComboColor
import com.reco1l.framework.*

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
    var sliderBorderColor: Color4? = null
}
