package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod

/**
 * Represents a [PlayableBeatmap] for [GameMode.Droid] game mode.
 */
class DroidPlayableBeatmap @JvmOverloads constructor(
    baseBeatmap: IBeatmap,
    mods: Iterable<Mod>? = null,
    customSpeedMultiplier: Float = 1f,
    oldStatistics: Boolean = false
) : PlayableBeatmap(baseBeatmap, GameMode.Droid, mods, customSpeedMultiplier, oldStatistics)