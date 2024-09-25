package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that make general adjustments to difficulty.
 *
 * This is used in place of [IModApplicableToDifficulty] to *forcefully* make adjustments that
 * correlates directly to other applied [Mod]s and settings.
 *
 * [Mod]s marked by this interface will have their adjustments performed after
 * [IModApplicableToDifficulty] [Mod]s have been applied.
 */
interface IModApplicableToDifficultyWithSettings {
    /**
     * Applies this [IModApplicableToDifficultyWithSettings] to a [BeatmapDifficulty].
     *
     * This is typically called post beatmap conversion.
     *
     * @param mode The [GameMode] to apply for.
     * @param difficulty The [BeatmapDifficulty] to mutate.
     * @param mods The [Mod]s that are used.
     * @param customSpeedMultiplier The custom speed multiplier that is used.
     */
    fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: List<Mod>, customSpeedMultiplier: Float)

}
