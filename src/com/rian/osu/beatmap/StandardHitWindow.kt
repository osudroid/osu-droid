package com.rian.osu.beatmap

import kotlin.math.floor

/**
 * Represents the osu!standard hit window.
 */
class StandardHitWindow @JvmOverloads constructor(
    /**
     * The overall difficulty of this [StandardHitWindow]. Defaults to 5.
     */
    overallDifficulty: Double? = 5.0
) : HitWindow(overallDifficulty) {
    /**
     * Creates a new [StandardHitWindow] with the specified overall difficulty.
     * The overall difficulty will be converted to a [Double].
     *
     * @param overallDifficulty The overall difficulty of this [StandardHitWindow]. Defaults to 5.
     */
    constructor(overallDifficulty: Float? = 5f) : this(overallDifficulty?.toDouble())

    override val greatWindow
        get() = floor(80 - 6 * overallDifficulty) - 0.5

    override val okWindow
        get() = floor(140 - 8 * overallDifficulty) - 0.5

    override val mehWindow
        get() = floor(200 - 10 * overallDifficulty) - 0.5
}