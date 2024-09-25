package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap

/**
 * An interface for [Mod]s that applies changes to a [Beatmap] after conversion and post-processing has completed.
 */
interface IModApplicableToBeatmap {
    /**
     * Applies this [IModApplicableToBeatmap] to a [Beatmap].
     *
     * @param beatmap The [Beatmap] to apply to.
     */
    fun applyToBeatmap(beatmap: Beatmap)
}
