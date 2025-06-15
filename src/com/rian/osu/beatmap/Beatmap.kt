package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.sliderobject.*
import com.rian.osu.beatmap.sections.*
import com.rian.osu.mods.IModApplicableToBeatmap
import com.rian.osu.mods.IModApplicableToDifficulty
import com.rian.osu.mods.IModApplicableToDifficultyWithMods
import com.rian.osu.mods.IModApplicableToHitObject
import com.rian.osu.mods.IModApplicableToHitObjectWithMods
import com.rian.osu.mods.IModFacilitatesAdjustment
import com.rian.osu.mods.IModRequiresOriginalBeatmap
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
) : IBeatmap, Cloneable {
    override var formatVersion = 14
    override val general = BeatmapGeneral()
    override val metadata = BeatmapMetadata()
    override var difficulty = BeatmapDifficulty()
    override val events = BeatmapEvents()
    override val colors = BeatmapColor()
    override val controlPoints = BeatmapControlPoints()
    override var hitObjects = BeatmapHitObjects()
    override var filePath = ""
    override var md5 = ""

    override val maxCombo by lazy {
        hitObjects.objects.sumOf {
            if (it is Slider) it.nestedHitObjects.size else 1
        }
    }

    /**
     * The maximum score of this [Beatmap].
     */
    val maxScore by lazy {
        var score = 0
        var combo = 0

        val difficultyMultiplier = 1 + difficulty.od / 10 + difficulty.hp / 10 + (difficulty.gameplayCS - 3) / 4

        for (obj in hitObjects.objects) {
            if (obj !is Slider) {
                score += (300 + (300 * combo * difficultyMultiplier) / 25).toInt()
                combo++
                continue
            }

            for (nested in obj.nestedHitObjects) {
                score += when (nested) {
                    is SliderHead, is SliderRepeat -> 30
                    is SliderTick -> 10
                    is SliderTail -> 300 + (300 * combo * difficultyMultiplier / 25).toInt()
                    else -> 0
                }

                combo++
            }
        }

        score
    }

    /**
     * Constructs a [DroidPlayableBeatmap] from this [Beatmap], where all [HitObject] and [BeatmapDifficulty]
     * [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [DroidPlayableBeatmap].
     */
    @JvmOverloads
    fun createDroidPlayableBeatmap(
        mods: Iterable<Mod>? = null,
        scope: CoroutineScope? = null
    ) = DroidPlayableBeatmap(createPlayableBeatmap(GameMode.Droid, mods, scope), mods)

    /**
     * Constructs a [StandardPlayableBeatmap] from this [Beatmap], where all [HitObject] and [BeatmapDifficulty]
     * [Mod]s have been applied, and [HitObject]s have been fully constructed.
     *
     * @param mods The [Mod]s to apply to the [Beatmap]. Defaults to No Mod.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The [StandardPlayableBeatmap].
     */
    @JvmOverloads
    fun createStandardPlayableBeatmap(
        mods: Iterable<Mod>? = null,
        scope: CoroutineScope? = null
    ) = StandardPlayableBeatmap(createPlayableBeatmap(GameMode.Standard, mods, scope), mods)

    private fun createPlayableBeatmap(mode: GameMode, mods: Iterable<Mod>?, scope: CoroutineScope?): Beatmap {
        if (this.mode == mode && mods?.firstOrNull() == null) {
            // Beatmap is already playable as is.
            return this
        }


        @Suppress("UNCHECKED_CAST")
        val adjustmentMods =
            (mods?.filter { it is IModFacilitatesAdjustment } ?: emptyList()) as Iterable<IModFacilitatesAdjustment>

        mods?.filterIsInstance<IModRequiresOriginalBeatmap>()?.forEach {
            scope?.ensureActive()
            it.applyFromBeatmap(this)
        }

        val converter = BeatmapConverter(this, scope)

        // Convert
        val converted = converter.convert()

        // Apply difficulty mods
        mods?.filterIsInstance<IModApplicableToDifficulty>()?.forEach {
            scope?.ensureActive()
            it.applyToDifficulty(mode, converted.difficulty, adjustmentMods)
        }

        mods?.filterIsInstance<IModApplicableToDifficultyWithMods>()?.forEach {
            scope?.ensureActive()
            it.applyToDifficulty(mode, converted.difficulty, mods)
        }

        val processor = BeatmapProcessor(converted, scope)

        processor.preProcess()

        // Compute default values for hit objects, including creating nested hit objects in-case they're needed
        converted.hitObjects.objects.forEach {
            scope?.ensureActive()
            it.applyDefaults(converted.controlPoints, converted.difficulty, mode, scope)
        }

        mods?.filterIsInstance<IModApplicableToHitObject>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                scope?.ensureActive()
                it.applyToHitObject(mode, obj, adjustmentMods)
            }
        }

        mods?.filterIsInstance<IModApplicableToHitObjectWithMods>()?.forEach {
            for (obj in converted.hitObjects.objects) {
                scope?.ensureActive()
                it.applyToHitObject(mode, obj, mods)
            }
        }

        processor.postProcess()

        mods?.filterIsInstance<IModApplicableToBeatmap>()?.forEach {
            scope?.ensureActive()
            it.applyToBeatmap(converted, scope)
        }

        return converted
    }

    public override fun clone() = super.clone() as Beatmap
}