package com.rian.osu.beatmap

import com.rian.osu.beatmap.sections.BeatmapColor
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.beatmap.sections.BeatmapEvents
import com.rian.osu.beatmap.sections.BeatmapGeneral
import com.rian.osu.beatmap.sections.BeatmapHitObjects
import com.rian.osu.beatmap.sections.BeatmapMetadata

/**
 * Represents a beatmap.
 */
interface IBeatmap {
    /**
     * The format version of this [IBeatmap].
     */
    val formatVersion: Int

    /**
     * The general section of this [IBeatmap].
     */
    val general: BeatmapGeneral

    /**
     * The metadata section of this [IBeatmap].
     */
    val metadata: BeatmapMetadata

    /**
     * The difficulty section of this [IBeatmap].
     */
    val difficulty: BeatmapDifficulty

    /**
     * The events section of this [IBeatmap].
     */
    val events: BeatmapEvents

    /**
     * The colors section of this [IBeatmap].
     */
    val colors: BeatmapColor

    /**
     * The control points of this [IBeatmap].
     */
    val controlPoints: BeatmapControlPoints

    /**
     * The hit objects of this [IBeatmap].
     */
    val hitObjects: BeatmapHitObjects

    /**
     * The path to the `.osu` file of this [IBeatmap].
     */
    val filePath: String

    /**
     * The path of the parent folder of this [IBeatmap].
     *
     * In other words, this is the beatmapset folder of this [IBeatmap].
     */
    val beatmapsetPath
        get() = filePath.substringBeforeLast("/")

    /**
     * The MD5 hash of this [IBeatmap].
     */
    val md5: String

    /**
     * The maximum combo of this [IBeatmap].
     */
    val maxCombo: Int

    /**
     * The duration of this [IBeatmap].
     */
    val duration
        get() = hitObjects.objects.lastOrNull()?.endTime?.toInt() ?: 0

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    fun getOffsetTime(time: Double) = time + if (formatVersion < 5) 24 else 0

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    fun getOffsetTime(time: Int) = time + if (formatVersion < 5) 24 else 0
}