package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.*
import com.rian.osu.mods.IModApplicableToBeatmap
import com.rian.osu.mods.IModApplicableToDifficulty
import com.rian.osu.mods.IModApplicableToDifficultyWithSettings
import com.rian.osu.mods.IModApplicableToHitObject
import com.rian.osu.mods.IModApplicableToHitObjectWithSettings
import com.rian.osu.mods.Mod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents a beatmap.
 */
open class Beatmap(
    /**
     * The [GameMode] this [Beatmap] was parsed as.
     */
    @JvmField
    val mode: GameMode
) : Cloneable {
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
    open var hitObjects = BeatmapHitObjects()

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
     * The max combo of this [Beatmap].
     */
    open val maxCombo by lazy {
        hitObjects.objects.sumOf {
            if (it is Slider) it.nestedHitObjects.size else 1
        }
    }

    /**
     * The duration of this [Beatmap].
     */
    val duration: Int
        get() = hitObjects.objects.lastOrNull()?.endTime?.toInt() ?: 0

    /**
     * Constructs a [DroidPlayableBeatmap] from this [Beatmap], where all [HitObject] and [BeatmapDifficulty]
     * [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param customSpeedMultiplier The custom speed multiplier to apply to the [Beatmap]. Defaults to 1.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [DroidPlayableBeatmap].
     */
    @JvmOverloads
    fun createDroidPlayableBeatmap(
        mods: Iterable<Mod>? = null,
        customSpeedMultiplier: Float = 1f,
        scope: CoroutineScope? = null
    ) = DroidPlayableBeatmap(createPlayableBeatmap(GameMode.Droid, mods, customSpeedMultiplier, scope), mods, customSpeedMultiplier)

    /**
     * Constructs a [StandardPlayableBeatmap] from this [Beatmap], where all [HitObject] and [BeatmapDifficulty]
     * [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param customSpeedMultiplier The custom speed multiplier to apply to the [Beatmap]. Defaults to 1.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [StandardPlayableBeatmap].
     */
    @JvmOverloads
    fun createStandardPlayableBeatmap(
        mods: Iterable<Mod>? = null,
        customSpeedMultiplier: Float = 1f,
        scope: CoroutineScope? = null
    ) = StandardPlayableBeatmap(createPlayableBeatmap(GameMode.Standard, mods, customSpeedMultiplier, scope), mods, customSpeedMultiplier)

    private fun createPlayableBeatmap(mode: GameMode, mods: Iterable<Mod>?, customSpeedMultiplier: Float, scope: CoroutineScope?): Beatmap {
        val converter = BeatmapConverter(this, scope)

        // Convert
        val converted = converter.convert()

        // Apply difficulty mods
        mods?.filterIsInstance<IModApplicableToDifficulty>()?.forEach {
            scope?.ensureActive()
            it.applyToDifficulty(mode, converted.difficulty)
        }

        mods?.filterIsInstance<IModApplicableToDifficultyWithSettings>()?.forEach {
            scope?.ensureActive()
            it.applyToDifficulty(mode, converted.difficulty, mods, customSpeedMultiplier)
        }

        val processor = BeatmapProcessor(converted, scope)

        processor.preProcess()

        // Compute default values for hit objects, including creating nested hit objects in-case they're needed
        converted.hitObjects.objects.forEach {
            scope?.ensureActive()
            it.applyDefaults(converted.controlPoints, converted.difficulty, mode)
        }

        mods?.filterIsInstance<IModApplicableToHitObject>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                scope?.ensureActive()
                it.applyToHitObject(mode, obj)
            }
        }

        mods?.filterIsInstance<IModApplicableToHitObjectWithSettings>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                scope?.ensureActive()
                it.applyToHitObject(mode, obj, mods, customSpeedMultiplier)
            }
        }

        processor.postProcess()

        mods?.filterIsInstance<IModApplicableToBeatmap>()?.forEach {
            scope?.ensureActive()
            it.applyToBeatmap(converted)
        }

        return converted
    }

    public override fun clone() = super.clone() as Beatmap
}