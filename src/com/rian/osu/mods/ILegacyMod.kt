package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents a legacy [Mod] that is no longer available to be selected by the user,
 * but is kept internally to maintain forwards compatibility.
 */
interface ILegacyMod : IModUserSelectable {
    /**
     * Migrates this [ILegacyMod] to a new [Mod].
     *
     * @param difficulty The [BeatmapDifficulty] to migrate this [ILegacyMod] against.
     * @return The new [Mod].
     */
    fun migrate(difficulty: BeatmapDifficulty): Mod
}