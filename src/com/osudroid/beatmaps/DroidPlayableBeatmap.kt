package com.osudroid.beatmaps

import com.osudroid.GameMode
import com.osudroid.mods.Mod
import com.osudroid.mods.ModPrecise

/**
 * Represents a [PlayableBeatmap] for [GameMode.Droid] game mode.
 */
class DroidPlayableBeatmap @JvmOverloads constructor(
    baseBeatmap: IBeatmap,
    mods: Iterable<Mod>? = null
) : PlayableBeatmap(baseBeatmap, GameMode.Droid, mods) {
    override fun createHitWindow() =
        if (ModPrecise::class in mods) PreciseDroidHitWindow(difficulty.od)
        else DroidHitWindow(difficulty.od)
}