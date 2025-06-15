package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModPrecise

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