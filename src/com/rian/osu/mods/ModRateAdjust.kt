package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.pow

/**
 * Represents a [Mod] that adjusts the track's rate.
 */
abstract class ModRateAdjust : Mod(), IModApplicableToTrackRate {
    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) =
        if (trackRateMultiplier > 1) 1 + (trackRateMultiplier - 1) * 0.24f
        else 0.3f.pow((1 - trackRateMultiplier) * 4)
}