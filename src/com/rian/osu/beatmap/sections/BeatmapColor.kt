package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.ComboColor
import ru.nsu.ccfit.zuev.osu.RGBColor

/**
 * Contains information about combo and skin colors of a beatmap.
 */
class BeatmapColor {
    /**
     * The combo colors of this beatmap.
     */
    @JvmField
    var comboColors = mutableListOf<ComboColor>()

    /**
     * The color of the slider border.
     */
    @JvmField
    var sliderBorderColor: RGBColor? = null
}
