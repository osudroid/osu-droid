package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod
import com.rian.osu.utils.ModUtils

/**
 * Represents a [Beatmap] that is in a playable state in a specific [GameMode].
 */
abstract class PlayableBeatmap @JvmOverloads constructor(
    beatmap: Beatmap,
    mode: GameMode,

    /**
     * The [Mod]s that were applied to this [PlayableBeatmap].
     */
    @JvmField
    val mods: Iterable<Mod>? = null,

    /**
     * The custom speed multiplier that was applied to this [PlayableBeatmap].
     */
    @JvmField
    val customSpeedMultiplier: Float = 1f
) : Beatmap(mode) {
    /**
     * The overall speed multiplier that was applied to this [PlayableBeatmap].
     */
    @JvmField
    val overallSpeedMultiplier = customSpeedMultiplier * if (mods != null) ModUtils.calculateRateWithMods(mods) else 1f

    init {
        formatVersion = beatmap.formatVersion
        general = beatmap.general
        metadata = beatmap.metadata
        difficulty = beatmap.difficulty
        events = beatmap.events
        colors = beatmap.colors
        controlPoints = beatmap.controlPoints
        hitObjects = beatmap.hitObjects
        filePath = beatmap.filePath
        md5 = beatmap.md5
    }
}