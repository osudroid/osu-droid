package com.rian.osu.mods

import com.reco1l.toolkt.*
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.settings.*
import kotlin.math.pow

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
        defaultValue = trackRateMultiplier,
        minValue = 0.5f,
        maxValue = 2f,
        step = 0.05f,
        precision = 2
    )


    final override val isRelevant
        get() = trackRateMultiplier != 1f

    final override val isValidForMultiplayerAsFreeMod = false

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) =
        if (trackRateMultiplier > 1) 1 + (trackRateMultiplier - 1) * 0.24f
        else 0.3f.pow((1 - trackRateMultiplier) * 4)

    final override fun applyToRate(time: Double, rate: Float) = rate * trackRateMultiplier
}