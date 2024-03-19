package com.rian.osu.beatmap

/**
 * Represents a hit window.
 */
abstract class HitWindow(
    /**
     * The overall difficulty of this [HitWindow].
     */
    @JvmField
    protected val overallDifficulty: Float
) {
    /**
     * The hit window for 300 (Great) hit result.
     */
    abstract val greatWindow: Float

    /**
     * The hit window for 100 (OK) hit result.
     */
    abstract val okWindow: Float

    /**
     * The hit window for 50 (Meh) hit result.
     */
    abstract val mehWindow: Float
}