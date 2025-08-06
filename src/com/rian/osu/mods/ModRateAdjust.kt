package com.rian.osu.mods

import com.reco1l.toolkt.*
import com.rian.osu.mods.settings.*

/**
 * Represents a [Mod] that adjusts the track's playback rate.
 */
sealed class ModRateAdjust(trackRateMultiplier: Float = 1f) : Mod(), IModApplicableToTrackRate {

    /**
     * The multiplier for the track's playback rate.
     */
    open var trackRateMultiplier by FloatModSetting(
        name = "Track rate multiplier",
        valueFormatter = { "${it.roundBy(2)}x" },
        defaultValue = 1f,
        minValue = 0.5f,
        maxValue = 2f,
        step = 0.05f,
        precision = 2
    )

    init {
        this.trackRateMultiplier = trackRateMultiplier
    }

    final override val isRelevant
        get() = trackRateMultiplier != 1f

    final override val isValidForMultiplayerAsFreeMod = false

    override val scoreMultiplier: Float
        get() = ModRateAdjustHelper(trackRateMultiplier).scoreMultiplier

    final override fun applyToRate(time: Double, rate: Float) = rate * trackRateMultiplier
}