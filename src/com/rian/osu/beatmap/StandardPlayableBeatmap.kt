package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod

/**
 * Represents a [PlayableBeatmap] for [GameMode.Standard] game mode.
 */
class StandardPlayableBeatmap @JvmOverloads constructor(
    baseBeatmap: IBeatmap,
    mods: Iterable<Mod>? = null,
    customSpeedMultiplier: Float = 1f
) : PlayableBeatmap(baseBeatmap, GameMode.Standard, mods, customSpeedMultiplier)