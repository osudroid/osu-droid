package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that make general adjustments to difficulty.
 *
 * This is used in place of [IModApplicableToDifficulty] to make adjustments that
 * correlates directly to other applied [Mod]s.
 *
 * [Mod]s marked by this interface will have their adjustments applied after
 * [IModApplicableToDifficulty] [Mod]s have been applied.
 */
interface IModApplicableToDifficultyWithMods {
    /**
     * Applies this [IModApplicableToDifficultyWithMods] to a [BeatmapDifficulty].
     *
     * This is typically called post beatmap conversion.
     *
     * @param mode The [GameMode] to apply for.
     * @param difficulty The [BeatmapDifficulty] to mutate.
     * @param mods The [Mod]s that are used.
     */
    fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: Iterable<Mod>)

}
