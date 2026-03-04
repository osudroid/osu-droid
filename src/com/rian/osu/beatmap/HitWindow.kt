package com.rian.osu.beatmap

/**
 * Represents a hit window.
 */
abstract class HitWindow @JvmOverloads constructor(
    /**
     * The overall difficulty of this [HitWindow]. Defaults to 5.
     */
    overallDifficulty: Double? = 5.0
) {
    /**
     * Creates a new [HitWindow] with the specified overall difficulty.
     * The overall difficulty will be converted to a [Double].
     *
     * @param overallDifficulty The overall difficulty of this [HitWindow]. Defaults to 5.
     */
    constructor(overallDifficulty: Float? = 5f) : this(overallDifficulty?.toDouble())

    /**
     * The overall difficulty of this [HitWindow].
     */
    @JvmField
    var overallDifficulty = overallDifficulty ?: 5.0

    /**
     * The hit window for 300 (Great) hit result in milliseconds.
     */
    abstract val greatWindow: Double

    /**
     * The hit window for 100 (OK) hit result in milliseconds.
     */
    abstract val okWindow: Double

    /**
     * The hit window for 50 (Meh) hit result in milliseconds.
     */
    abstract val mehWindow: Double

    companion object {
        /**
         * A fixed miss window regardless of difficulty settings in milliseconds.
         */
        const val MISS_WINDOW = 400.0
    }
}