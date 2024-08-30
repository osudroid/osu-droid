package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.getEndTime
import com.rian.osu.beatmap.sections.*
import com.rian.osu.mods.IApplicableToBeatmap
import com.rian.osu.mods.IApplicableToDifficulty
import com.rian.osu.mods.IApplicableToDifficultyWithSettings
import com.rian.osu.mods.IApplicableToHitObject
import com.rian.osu.mods.Mod

/**
 * Represents a beatmap.
 */
class Beatmap : Cloneable {
    /**
     * The format version of this [Beatmap].
     */
    @JvmField
    var formatVersion = 14

    /**
     * The general section of this [Beatmap].
     */
    @JvmField
    var general = BeatmapGeneral()

    /**
     * The metadata section of this [Beatmap].
     */
    @JvmField
    var metadata = BeatmapMetadata()

    /**
     * The difficulty section of this [Beatmap].
     */
    @JvmField
    var difficulty = BeatmapDifficulty()

    /**
     * The events section of this [Beatmap].
     */
    @JvmField
    var events = BeatmapEvents()

    /**
     * The colors section of this [Beatmap].
     */
    @JvmField
    var colors = BeatmapColor()

    /**
     * The control points of this [Beatmap].
     */
    @JvmField
    var controlPoints = BeatmapControlPoints()

    /**
     * The hit objects of this [Beatmap].
     */
    @JvmField
    var hitObjects = BeatmapHitObjects()

    /**
     * The path to the `.osu` file of this [Beatmap].
     */
    @JvmField
    var filePath = ""

    /**
     * The path of the parent folder of this [Beatmap].
     *
     * In other words, this is the beatmapset folder of this [Beatmap].
     */
    val beatmapsetPath
        get() = filePath.substringBeforeLast("/")

    /**
     * The MD5 hash of this [Beatmap].
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
     * Gets the max combo of this [Beatmap].
     */
    val maxCombo by lazy {
        hitObjects.objects.sumOf {
            if (it is Slider) it.nestedHitObjects.size else 1
        }
    }

    /**
     * The duration of this [Beatmap].
     */
    val duration: Int
        get() = hitObjects.objects.lastOrNull()?.getEndTime()?.toInt() ?: 0

    /**
     * Constructs a playable [Beatmap] from this [Beatmap].
     *
     * The returned [Beatmap] is in a playable state - all [HitObject] and [BeatmapDifficulty] [Mod]s have been applied,
     * and [HitObject]s have been fully constructed.
     *
     * @param mode The [GameMode] to construct the [Beatmap] for.
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param customSpeedMultiplier The custom speed multiplier to apply to the [Beatmap]. Defaults to 1.
     * @return The constructed [Beatmap].
     */
    @JvmOverloads
    fun createPlayableBeatmap(mode: GameMode, mods: List<Mod>? = null, customSpeedMultiplier: Float = 1f): Beatmap {
        val converter = BeatmapConverter(this)

        // Convert
        val converted = converter.convert()

        // Apply difficulty mods
        mods?.filterIsInstance<IApplicableToDifficulty>()?.forEach {
            it.applyToDifficulty(mode, converted.difficulty)
        }

        mods?.filterIsInstance<IApplicableToDifficultyWithSettings>()?.forEach {
            it.applyToDifficulty(mode, converted.difficulty, mods, customSpeedMultiplier)
        }

        val processor = BeatmapProcessor(converted)

        processor.preProcess()

        // Compute default values for hit objects, including creating nested hit objects in-case they're needed
        converted.hitObjects.objects.forEach {
            it.applyDefaults(converted.controlPoints, converted.difficulty, mode)
        }

        mods?.filterIsInstance<IApplicableToHitObject>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                it.applyToHitObject(mode, obj)
            }
        }

        processor.postProcess(mode)

        mods?.filterIsInstance<IApplicableToBeatmap>()?.forEach {
            it.applyToBeatmap(converted)
        }

        return converted
    }

    public override fun clone() = super.clone() as Beatmap
}