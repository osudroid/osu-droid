package com.rian.osu.beatmap

import com.reco1l.framework.*

/**
 * A wrapper of [ColorARGB] specifically for combo colors.
 */
data class ComboColor(
    /**
     * The index of this combo color.
     */
    @JvmField val index: Int,

    /**
     * The wrapped [ColorARGB].
     */
    val color: ColorARGB
)
