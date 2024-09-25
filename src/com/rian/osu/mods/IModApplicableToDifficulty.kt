package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty

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
     */
    fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty)
}