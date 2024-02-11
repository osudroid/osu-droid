package com.rian.osu.beatmap.sections

import com.rian.osu.beatmap.ComboColor
import ru.nsu.ccfit.zuev.osu.RGBColor

/**
 * Contains information about combo and skin colors of a beatmap.
 */
class BeatmapColor : Cloneable {
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

    public override fun clone() =
        (super.clone() as BeatmapColor).also {
            it.comboColors = mutableListOf()
            comboColors.forEach { c -> it.comboColors.add(c.clone()) }

            it.sliderBorderColor = sliderBorderColor?.let { RGBColor(it) }
        }
}
