package com.osudroid.difficulty.attributes

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.Slider

/**
 * Represents a [Slider] that is considered difficult.
 */
data class DifficultSlider(
    /**
     * The index of the [Slider] in the [Beatmap].
     */
    @JvmField
    val index: Int,

    /**
     * The difficulty rating of this [Slider] compared to other [Slider]s, based on the velocity of the [Slider].
     *
     * A value closer to 1 indicates that this [Slider] is more difficult compared to most [Slider]s.
     *
     * A value closer to 0 indicates that this [Slider] is easier compared to most [Slider]s.
     */
    @JvmField
    val difficultyRating: Double
)