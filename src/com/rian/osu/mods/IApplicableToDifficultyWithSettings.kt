package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that make adjustments to difficulty based on other applied [Mod]s and settings.
 *
 * This should not be used together with [IApplicableToDifficulty].
 */
interface IApplicableToDifficultyWithSettings {
    /**
     * Applies this [IApplicableToDifficultyWithSettings] to a [BeatmapDifficulty].
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
