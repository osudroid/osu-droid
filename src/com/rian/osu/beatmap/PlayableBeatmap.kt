package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod
import com.rian.osu.utils.ModHashMap
import com.rian.osu.utils.ModUtils

/**
 * Represents an [IBeatmap] that is in a playable state in a specific [GameMode].
 */
abstract class PlayableBeatmap @JvmOverloads constructor(
    baseBeatmap: IBeatmap,

    /**
     * The [GameMode] this [PlayableBeatmap] was parsed as.
     */
    @JvmField
    val mode: GameMode,

    mods: Iterable<Mod>? = null,
) : IBeatmap {
    /**
     * The [Mod]s that were applied to this [PlayableBeatmap].
     */
    @JvmField
    val mods = ModHashMap(mods)

    override val formatVersion = baseBeatmap.formatVersion
    override val general = baseBeatmap.general
    override val metadata = baseBeatmap.metadata
    override val difficulty = baseBeatmap.difficulty
    override val events = baseBeatmap.events
    override val colors = baseBeatmap.colors
    override val controlPoints = baseBeatmap.controlPoints
    override val hitObjects = baseBeatmap.hitObjects
    override val filePath = baseBeatmap.filePath
    override val md5 = baseBeatmap.md5
    override val maxCombo = baseBeatmap.maxCombo

    /**
     * The speed multiplier that was applied to this [PlayableBeatmap].
     */
    @JvmField
    val speedMultiplier = if (mods != null) ModUtils.calculateRateWithMods(mods) else 1f
}