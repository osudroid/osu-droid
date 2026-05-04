package com.osudroid.mods

import com.osudroid.beatmaps.Beatmap

/**
 * An interface for [Mod]s that require the original instance of a [Beatmap] to perform conversion and processing.
 */
interface IModRequiresOriginalBeatmap {
    /**
     * Applies this [IModRequiresOriginalBeatmap] from a [Beatmap].
     *
     * This is called before conversion and processing.
     *
     * @param beatmap The [Beatmap] to apply from.
     */
    fun applyFromBeatmap(beatmap: Beatmap)
}