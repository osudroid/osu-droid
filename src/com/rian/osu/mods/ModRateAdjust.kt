package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.pow

/**
 * Represents a [Mod] that adjusts the track's playback rate.
 */
abstract class ModRateAdjust(
    /**
     * The multiplier for the track's playback rate.
     */
    var trackRateMultiplier: Float
) : Mod(), IModApplicableToTrackRate {
    override val isRelevant
        get() = trackRateMultiplier != 1f

    override val isValidForMultiplayerAsFreeMod = false

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) =
        if (trackRateMultiplier > 1) 1 + (trackRateMultiplier - 1) * 0.24f
        else 0.3f.pow((1 - trackRateMultiplier) * 4)

    override fun applyToRate(time: Double, rate: Float) = rate * trackRateMultiplier

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