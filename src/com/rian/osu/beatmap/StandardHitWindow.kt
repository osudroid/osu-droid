package com.rian.osu.beatmap

import kotlin.math.floor

/**
 * Represents the osu!standard hit window.
 */
class StandardHitWindow @JvmOverloads constructor(
    /**
     * The overall difficulty of this [StandardHitWindow]. Defaults to 5.
     */
    overallDifficulty: Float? = 5f
) : HitWindow(overallDifficulty) {
    override val greatWindow
        get() = floor(80 - 6 * overallDifficulty) - 0.5f

    override val okWindow
        get() = floor(140 - 8 * overallDifficulty) - 0.5f

    override val mehWindow
        get() = floor(200 - 10 * overallDifficulty) - 0.5f
}