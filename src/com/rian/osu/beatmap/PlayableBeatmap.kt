package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModNightCore
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

    /**
     * The [Mod]s that were applied to this [PlayableBeatmap].
     */
    @JvmField
    val mods: Iterable<Mod>? = null,

    /**
     * The custom speed multiplier that was applied to this [PlayableBeatmap].
     */
    @JvmField
    val customSpeedMultiplier: Float = 1f,

    /**
     * Whether to enforce old statistics.
     *
     * Some [Mod]s behave differently with this flag. For example, [ModNightCore] will apply a 1.39 rate multiplier
     * instead of 1.5 when this is `true`. **Never set this flag to `true` unless you know what you are doing.**
     */
    @JvmField
    val oldStatistics: Boolean = false
) : IBeatmap {
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
     * The overall speed multiplier that was applied to this [PlayableBeatmap].
     */
    @JvmField
    val overallSpeedMultiplier =
        customSpeedMultiplier * if (mods != null) ModUtils.calculateRateWithMods(mods, oldStatistics) else 1f
}