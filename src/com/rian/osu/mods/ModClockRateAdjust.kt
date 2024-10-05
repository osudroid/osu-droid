package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.pow

/**
 * Represents a [Mod] that adjusts the track's rate.
 */
abstract class ModClockRateAdjust : Mod(), IModApplicableToTrackRate {
    override val isValidForMultiplayerAsFreeMod = false

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) =
        if (trackRateMultiplier > 1) 1 + (trackRateMultiplier - 1) * 0.24f
        else 0.3f.pow((1 - trackRateMultiplier) * 4)

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModClockRateAdjust) {
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