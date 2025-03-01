package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModPrecise

/**
 * Represents a [PlayableBeatmap] for [GameMode.Droid] game mode.
 */
class DroidPlayableBeatmap @JvmOverloads constructor(
    baseBeatmap: IBeatmap,
    mods: Iterable<Mod>? = null,
    customSpeedMultiplier: Float = 1f,
    oldStatistics: Boolean = false
) : PlayableBeatmap(baseBeatmap, GameMode.Droid, mods, customSpeedMultiplier, oldStatistics) {
    override fun createHitWindow() =
        if (mods?.any { it is ModPrecise } == true) PreciseDroidHitWindow(difficulty.od)
        else DroidHitWindow(difficulty.od)
}