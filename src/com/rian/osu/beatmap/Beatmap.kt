package com.rian.osu.beatmap

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.getEndTime
import com.rian.osu.beatmap.sections.*

// TODO: support beatmap conversion

/**
 * Represents a beatmap.
 */
class Beatmap : Cloneable {
    /**
     * The format version of this beatmap.
     */
    @JvmField
    var formatVersion = 14

    /**
     * The general section of this beatmap.
     */
    @JvmField
    var general = BeatmapGeneral()

    /**
     * The metadata section of this beatmap.
     */
    @JvmField
    var metadata = BeatmapMetadata()

    /**
     * The difficulty section of this beatmap.
     */
    @JvmField
    var difficulty = BeatmapDifficulty()

    /**
     * The events section of this beatmap.
     */
    @JvmField
    var events = BeatmapEvents()

    /**
     * The colors section of this beatmap.
     */
    @JvmField
    var colors = BeatmapColor()

    /**
     * The control points of this beatmap.
     */
    @JvmField
    var controlPoints = BeatmapControlPoints()

    /**
     * The hit objects of this beatmap.
     */
    @JvmField
    var hitObjects = BeatmapHitObjects()

    /**
     * Raw timing points data.
     */
    @JvmField
    var rawTimingPoints = mutableListOf<String>()

    /**
     * Raw hit objects data.
     */
    @JvmField
    var rawHitObjects = mutableListOf<String>()

    /**
     * The path of parent folder of this beatmap.
     */
    @JvmField
    var folder: String? = null

    /**
     * The name of the `.osu` file of this beatmap.
     */
    @JvmField
    var filename = ""

    /**
     * The MD5 hash of this beatmap.
     */
    @JvmField
    var md5 = ""

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

    /**
     * Gets the max combo of this beatmap.
     */
    val maxCombo by lazy {
        hitObjects.objects.sumOf {
            if (it is Slider) it.nestedHitObjects.size else 1
        }
    }

    /**
     * The duration of this beatmap.
     */
    val duration: Int
        get() = hitObjects.objects.lastOrNull()?.getEndTime()?.toInt() ?: 0

    public override fun clone() =
        (super.clone() as Beatmap).apply {
            general = this@Beatmap.general.copy()
            metadata = this@Beatmap.metadata.copy()
            difficulty = this@Beatmap.difficulty.clone()
            events = this@Beatmap.events.clone()
            colors = this@Beatmap.colors.clone()
            controlPoints = this@Beatmap.controlPoints.clone()
            hitObjects = this@Beatmap.hitObjects.clone()
        }
}