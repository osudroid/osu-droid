package com.osudroid.mods

import com.osudroid.beatmaps.sections.BeatmapDifficulty

/**
 * An interface for [Mod]s that are no longer available to be selected by the user, but can be migrated into a new [Mod].
 */
interface IMigratableMod {
    /**
     * Migrates this [IMigratableMod] to a new [Mod].
     *
     * @param difficulty The [BeatmapDifficulty] to migrate this [IMigratableMod] against.
     * @return The new [Mod].
     */
    fun migrate(difficulty: BeatmapDifficulty): Mod
}