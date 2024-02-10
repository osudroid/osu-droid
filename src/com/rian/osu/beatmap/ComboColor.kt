package com.rian.osu.beatmap

import ru.nsu.ccfit.zuev.osu.RGBColor

/**
 * An extension to [RGBColor] specifically for combo colors.
 */
class ComboColor(
    /**
     * The index of this combo color.
     */
    @JvmField val index: Int,

    /**
     * The underlying [RGBColor].
     */
    color: RGBColor
) : RGBColor(color), Cloneable {
    public override fun clone() = super.clone() as ComboColor
}
