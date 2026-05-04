package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that make general adjustments to difficulty.
 */
interface IModApplicableToDifficulty {
    /**
     * Applies this [IModApplicableToDifficulty] to a [BeatmapDifficulty].
     *
     * This is typically called post beatmap conversion.
     *
     * @param mode The [GameMode] to apply for.
     * @param difficulty The [BeatmapDifficulty] to mutate.
     * @param adjustmentMods [Mod]s that apply [IModFacilitatesAdjustment].
     */
    fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    )
}