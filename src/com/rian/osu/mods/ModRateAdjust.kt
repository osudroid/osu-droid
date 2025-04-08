package com.rian.osu.mods

import com.reco1l.toolkt.*
import com.rian.osu.beatmap.sections.BeatmapDifficulty
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
        minValue = 0.1f,
        maxValue = 2f,
        step = 0.1f
    )


    final override val isRelevant
        get() = trackRateMultiplier != 1f

    final override val isValidForMultiplayerAsFreeMod = false

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) =
        if (trackRateMultiplier > 1) 1 + (trackRateMultiplier - 1) * 0.24f
        else 0.3f.pow((1 - trackRateMultiplier) * 4)

    final override fun applyToRate(time: Double, rate: Float) = rate * trackRateMultiplier

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModRateAdjust) {
            return false
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()

        result = 31 * result + trackRateMultiplier.hashCode()

        return result
    }
}