package com.rian.osu.mods

import kotlin.math.pow

/**
 * Helper class for [Mod]s related to track rate adjustments.
 *
 * @param trackRateMultiplier The multiplier for the track's playback rate.
 */
class ModRateAdjustHelper(private val trackRateMultiplier: Float) {
    /**
     * The score multiplier based on the track rate multiplier.
     */
    val scoreMultiplier: Float
        get() = if (trackRateMultiplier > 1) 1 + (trackRateMultiplier - 1) * 0.24f
                else 0.3f.pow((1 - trackRateMultiplier) * 4)
}