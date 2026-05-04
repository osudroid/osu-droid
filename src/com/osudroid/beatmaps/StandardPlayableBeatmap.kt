package com.osudroid.beatmaps

import com.osudroid.GameMode
import com.osudroid.mods.Mod

/**
 * Represents a [PlayableBeatmap] for [GameMode.Standard] game mode.
 */
class StandardPlayableBeatmap @JvmOverloads constructor(
    baseBeatmap: IBeatmap,
    mods: Iterable<Mod>? = null
) : PlayableBeatmap(baseBeatmap, GameMode.Standard, mods) {
    override fun createHitWindow() = StandardHitWindow(difficulty.od)
}