package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.IBeatmap
import com.rian.osu.beatmap.StandardPlayableBeatmap
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTail
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTick
import com.rian.osu.difficulty.attributes.*
import com.rian.osu.difficulty.calculator.*
import com.rian.osu.mods.Mod
import com.rian.osu.replay.SliderCheeseChecker
import com.rian.osu.replay.ThreeFingerChecker
import com.rian.osu.replay.createCursorGroups
import com.rian.osu.utils.LRUCache
import com.rian.osu.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.scoring.Replay.MoveArray
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

private val droidDifficultyCalculator = DroidDifficultyCalculator()
private val standardDifficultyCalculator = StandardDifficultyCalculator()

/**
 * A helper class for operations relating to difficulty and performance calculation.
 */
object BeatmapDifficultyCalculator {
    /**
     * Cache of difficulty calculations, mapped by MD5 hash of a beatmap.
     */
    private val difficultyCacheManager = LRUCache<String, BeatmapDifficultyCacheManager>(10)

    /**
     * Constructs a [DroidPerformanceCalculationParameters] from an [IBeatmap] and [StatisticV2].
     *
     * @param beatmap The [IBeatmap] to construct the [DroidPerformanceCalculationParameters] from.
     * @param stat The [StatisticV2] to construct the [DroidPerformanceCalculationParameters] from.
     * @return The [DroidPerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructDroidPerformanceParameters(beatmap: IBeatmap, stat: StatisticV2?) = stat?.run {
        DroidPerformanceCalculationParameters().also {
            populateParameters(it, beatmap, this)
        }
    }

    /**
     * Constructs a [PerformanceCalculationParameters] from an [IBeatmap] and [StatisticV2].
     *
     * @param beatmap The [IBeatmap] to construct the [PerformanceCalculationParameters] from.
     * @param stat The [StatisticV2] to construct the [PerformanceCalculationParameters] from.
     * @return The [PerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructStandardPerformanceParameters(beatmap: IBeatmap, stat: StatisticV2?) = stat?.run {
        PerformanceCalculationParameters().also {
            populateParameters(it, beatmap, this)
        }
    }

    private fun populateParameters(parameters: PerformanceCalculationParameters, beatmap: IBeatmap, stat: StatisticV2) {
        parameters.maxCombo = stat.getScoreMaxCombo()
        parameters.countGreat = stat.hit300
        parameters.countOk = stat.hit100
        parameters.countMeh = stat.hit50
        parameters.countMiss = stat.misses

        parameters.sliderTicksMissed =
            if (stat.sliderTickHits >= 0) beatmap.hitObjects.sliderTickCount - stat.sliderTickHits else null

        parameters.sliderEndsDropped =
            if (stat.sliderEndHits >= 0) beatmap.hitObjects.sliderCount - stat.sliderEndHits else null
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficulty(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(mods) ?:
        droidDifficultyCalculator.calculate(beatmap, mods, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the difficulty of a [DroidPlayableBeatmap].
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [DroidPlayableBeatmap].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficulty(beatmap: DroidPlayableBeatmap, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(beatmap.mods.values) ?:
        droidDifficultyCalculator.calculate(beatmap, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s, returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [Beatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of
     * the [Beatmap] at any relevant time relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidTimedDifficulty(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidTimedDifficultyCache(mods) ?:
        droidDifficultyCalculator.calculateTimed(beatmap, mods, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the difficulty of a [DroidPlayableBeatmap], returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [DroidPlayableBeatmap] at any relevant time.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of the [DroidPlayableBeatmap]
     * at any relevant time.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidTimedDifficulty(beatmap: DroidPlayableBeatmap, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidTimedDifficultyCache(beatmap.mods.values) ?:
        droidDifficultyCalculator.calculateTimed(beatmap, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @return A structure describing the osu!standard difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardDifficulty(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardDifficultyCache(mods) ?:
        standardDifficultyCalculator.calculate(beatmap, mods, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the difficulty of a [StandardPlayableBeatmap].
     *
     * @param beatmap The [StandardPlayableBeatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!standard difficulty of the [StandardPlayableBeatmap].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardDifficulty(beatmap: StandardPlayableBeatmap, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardDifficultyCache(beatmap.mods.values) ?:
        standardDifficultyCalculator.calculate(beatmap, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s, returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [Beatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of
     * the [Beatmap] at any relevant time relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardTimedDifficulty(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null) =
        calculateStandardTimedDifficulty(beatmap.createStandardPlayableBeatmap(mods, scope), scope)

    /**
     * Calculates the difficulty of a [StandardPlayableBeatmap], returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [StandardPlayableBeatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of the [StandardPlayableBeatmap]
     * at any relevant time.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardTimedDifficulty(beatmap: StandardPlayableBeatmap, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardTimedDifficultyCache(beatmap.mods.values) ?:
        standardDifficultyCalculator.calculateTimed(beatmap, scope).also { addCache(beatmap, it) }

    /**
     * Calculates the performance of a [DroidDifficultyAttributes].
     *
     * @param beatmap The [IBeatmap] associated with the [DroidDifficultyAttributes].
     * @param attributes The [DroidDifficultyAttributes] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateDroidPerformance(beatmap: IBeatmap, attributes: DroidDifficultyAttributes, stat: StatisticV2) =
        calculateDroidPerformance(attributes, constructDroidPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [DroidDifficultyAttributes].
     *
     * @param attributes The [DroidDifficultyAttributes] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidPerformance(
        attributes: DroidDifficultyAttributes,
        parameters: DroidPerformanceCalculationParameters? = null
    ) = DroidPerformanceCalculator(attributes).calculate(parameters)

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [Beatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithStat")
    fun calculateDroidPerformance(
        beatmap: Beatmap,
        attributes: DroidDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        stat: StatisticV2? = null
    ) = calculateDroidPerformance(beatmap, attributes, replayMovements, replayObjectData, constructDroidPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [Beatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithParameters")
    fun calculateDroidPerformance(
        beatmap: Beatmap,
        attributes: DroidDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        parameters: DroidPerformanceCalculationParameters? = null
    ) = calculateDroidPerformance(
            beatmap.createDroidPlayableBeatmap(attributes.mods),
            attributes, replayMovements, replayObjectData, parameters
        )

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [DroidPlayableBeatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithReplayStat")
    fun calculateDroidPerformance(
        beatmap: DroidPlayableBeatmap,
        attributes: DroidDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        stat: StatisticV2? = null
    ) = calculateDroidPerformance(beatmap, attributes, replayMovements, replayObjectData, constructDroidPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [DroidPlayableBeatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithReplayParameters")
    fun calculateDroidPerformance(
        beatmap: DroidPlayableBeatmap,
        attributes: DroidDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        parameters: DroidPerformanceCalculationParameters? = null
    ): DroidPerformanceAttributes {
        val actualParameters =
            (parameters ?: DroidPerformanceCalculationParameters()).also {
                val cursorGroups = createCursorGroups(replayMovements)

                it.tapPenalty = ThreeFingerChecker(
                    beatmap, attributes, cursorGroups, replayObjectData
                ).calculatePenalty()

                it.sliderCheesePenalty = SliderCheeseChecker(
                    beatmap, attributes, cursorGroups, replayObjectData
                ).calculatePenalty()

                it.populateNestedSliderObjectParameters(beatmap, replayObjectData)
            }

        return DroidPerformanceCalculator(attributes).calculate(actualParameters)
    }

    /**
     * Calculates the performance of a [StandardDifficultyAttributes].
     *
     * @param beatmap The [IBeatmap] associated with the [StandardDifficultyAttributes].
     * @param attributes The [StandardDifficultyAttributes] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [StandardDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateStandardPerformance(beatmap: IBeatmap, attributes: StandardDifficultyAttributes, stat: StatisticV2) =
        calculateStandardPerformance(attributes, constructStandardPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [StandardDifficultyAttributes].
     *
     * @param attributes The [StandardDifficultyAttributes] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [StandardDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardPerformance(
        attributes: StandardDifficultyAttributes,
        parameters: PerformanceCalculationParameters? = null
    ) = StandardPerformanceCalculator(attributes).calculate(parameters)

    /**
     * Calculates the performance of a [StandardDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [StandardPlayableBeatmap] to calculate.
     * @param attributes The [StandardDifficultyAttributes] of the [Beatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [StandardDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateStandardPerformanceWithReplayStat")
    fun calculateStandardPerformance(
        beatmap: StandardPlayableBeatmap,
        attributes: StandardDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        stat: StatisticV2? = null
    ) = calculateStandardPerformance(beatmap, attributes, replayMovements, replayObjectData, constructStandardPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [StandardDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param attributes The [StandardDifficultyAttributes] of the [Beatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [StandardDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateStandardPerformanceWithParameters")
    fun calculateStandardPerformance(
        beatmap: Beatmap,
        attributes: StandardDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        parameters: PerformanceCalculationParameters? = null
    ) = calculateStandardPerformance(
            beatmap.createStandardPlayableBeatmap(attributes.mods),
            attributes, replayMovements, replayObjectData, parameters
        )

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [DroidPlayableBeatmap].
     * @param replayMovements The replay movements of the player.
     * @param replayObjectData The replay object data of the player.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateStandardPerformanceWithReplayParameters")
    fun calculateStandardPerformance(
        beatmap: StandardPlayableBeatmap,
        attributes: StandardDifficultyAttributes,
        replayMovements: List<MoveArray>,
        replayObjectData: Array<ReplayObjectData>,
        parameters: PerformanceCalculationParameters? = null
    ): StandardPerformanceAttributes {
        val actualParameters =
            (parameters ?: PerformanceCalculationParameters()).also {
                it.populateNestedSliderObjectParameters(beatmap, replayObjectData)
            }

        return StandardPerformanceCalculator(attributes).calculate(actualParameters)
    }

    /**
     * Invalidates expired cache.
     */
    @JvmStatic
    fun invalidateExpiredCache() = difficultyCacheManager.entries.iterator().run {
        val currentTime = System.currentTimeMillis()

        while (hasNext()) {
            next().value.let {
                it.invalidateExpiredCache(currentTime)

                if (it.isEmpty) {
                    remove()
                }
            }
        }
    }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(beatmap: IBeatmap, attributes: DroidDifficultyAttributes) =
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes, 60 * 1000) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(beatmap: IBeatmap, attributes: StandardDifficultyAttributes) =
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes, 60 * 1000) }

    private fun PerformanceCalculationParameters.populateNestedSliderObjectParameters(
        beatmap: IBeatmap,
        replayObjectData: Array<ReplayObjectData>,
        scope: CoroutineScope? = null
    ) {
        sliderEndsDropped = 0
        sliderTicksMissed = 0

        val objects = beatmap.hitObjects.objects

        for (i in objects.indices) {
            scope?.ensureActive()

            val obj = objects[i] as? Slider ?: continue
            val objData = replayObjectData.getOrNull(i)

            if (objData?.tickSet == null) {
                // No object data - assume all slider ticks and the end were dropped.
                sliderEndsDropped = (sliderEndsDropped ?: 0) + 1
                sliderTicksMissed = (sliderTicksMissed ?: 0) + obj.nestedHitObjects.size - 2 - obj.repeatCount
                continue
            }

            for (j in 1 until obj.nestedHitObjects.size) {
                scope?.ensureActive()

                if (objData.tickSet[j - 1]) {
                    continue
                }

                when (obj.nestedHitObjects[j]) {
                    is SliderTick -> sliderTicksMissed = (sliderTicksMissed ?: 0) + 1
                    is SliderTail -> sliderEndsDropped = (sliderEndsDropped ?: 0) + 1
                }
            }
        }
    }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addDroidTimedCache")
    private fun addCache(
        beatmap: IBeatmap,
        attributes: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>
    ) =
        // Allow a maximum of 5 minutes of living cache.
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes, min(
            beatmap.duration.toLong(),
            5 * 60 * 1000
        )) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addStandardTimedCache")
    private fun addCache(
        beatmap: IBeatmap,
        attributes: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>
    ) =
        // Allow a maximum of 5 minutes of living cache.
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes, min(
            beatmap.duration.toLong(),
            5 * 60 * 1000
        )) }
}

/**
 * A cache holder for a [Beatmap].
 */
private class BeatmapDifficultyCacheManager {
    private val droidAttributeCache =
        LRUCache<Set<Mod>, BeatmapDifficultyCache<DroidDifficultyAttributes>>(5)
    private val droidTimedAttributeCache =
        LRUCache<Set<Mod>, BeatmapDifficultyCache<Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>>>(3)
    private val standardAttributeCache =
        LRUCache<Set<Mod>, BeatmapDifficultyCache<StandardDifficultyAttributes>>(5)
    private val standardTimedAttributeCache =
        LRUCache<Set<Mod>, BeatmapDifficultyCache<Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>>>(3)

    /**
     * Adds a [DroidDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The [DroidDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    fun addCache(attributes: DroidDifficultyAttributes, timeToLive: Long) =
        addCache(attributes.mods, GameMode.Droid, attributes, droidAttributeCache, timeToLive)

    /**
     * Adds a [StandardDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The [StandardDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    fun addCache(attributes: StandardDifficultyAttributes, timeToLive: Long) =
        addCache(attributes.mods, GameMode.Standard, attributes, standardAttributeCache, timeToLive)

    /**
     * Adds a set of [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The set of [TimedDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    @JvmName("addDroidTimedCache")
    fun addCache(attributes: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>, timeToLive: Long) =
        addCache(attributes.first().attributes.mods, GameMode.Droid, attributes, droidTimedAttributeCache, timeToLive)

    /**
     * Adds a set of [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The set of [TimedDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    @JvmName("addStandardTimedCache")
    fun addCache(attributes: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>, timeToLive: Long) =
        addCache(attributes.first().attributes.mods, GameMode.Standard, attributes, standardTimedAttributeCache, timeToLive)

    /**
     * Retrieves the [DroidDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [DroidDifficultyAttributes], `null` if not found.
     */
    fun getDroidDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Droid, droidAttributeCache)

    /**
     * Retrieves the [TimedDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [TimedDifficultyAttributes], `null` if not found.
     */
    fun getDroidTimedDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Droid, droidTimedAttributeCache)

    /**
     * Retrieves the [StandardDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [StandardDifficultyAttributes], `null` if not found.
     */
    fun getStandardDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Standard, standardAttributeCache)

    /**
     * Retrieves the [TimedDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [TimedDifficultyAttributes], `null` if not found.
     */
    fun getStandardTimedDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Standard, standardTimedAttributeCache)

    /**
     * Whether this [BeatmapDifficultyCacheManager] does not hold any cache.
     */
    val isEmpty
        get() = droidAttributeCache.isEmpty() && droidTimedAttributeCache.isEmpty() &&
                standardAttributeCache.isEmpty() && standardTimedAttributeCache.isEmpty()

    /**
     * Invalidates all expired cache in this manager.
     *
     * @param currentTime The time to invalidate the cache against, in milliseconds.
     */
    fun invalidateExpiredCache(currentTime: Long) {
        invalidateExpiredCache(currentTime, droidAttributeCache)
        invalidateExpiredCache(currentTime, droidTimedAttributeCache)
        invalidateExpiredCache(currentTime, standardAttributeCache)
        invalidateExpiredCache(currentTime, standardTimedAttributeCache)
    }

    /**
     * Invalidates all expired cache of a cache map in this manager.
     *
     * @param currentTime The time to invalidate the cache against, in milliseconds.
     * @param cacheMap The map.
     */
    private fun <T> invalidateExpiredCache(
        currentTime: Long,
        cacheMap: HashMap<Set<Mod>, BeatmapDifficultyCache<T>>
    ) = cacheMap.iterator().run {
            for ((_, value) in this) {
                if (value.isExpired(currentTime)) {
                    remove()
                }
            }
        }

    /**
     * Adds a difficulty attributes cache to a cache map.
     *
     * @param mods The [ModHashMap] to cache for.
     * @param mode The [GameMode] to get for.
     * @param cache The difficulty attributes cache to add.
     * @param cacheMap The map to add the cache to.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    private fun <T> addCache(
        mods: Iterable<Mod>?, mode: GameMode, cache: T,
        cacheMap: HashMap<Set<Mod>, BeatmapDifficultyCache<T>>,
        timeToLive: Long
    ) {
        cacheMap[processMods(mods, mode)] = BeatmapDifficultyCache(cache, timeToLive)
    }

    /**
     * Gets the cache of difficulty attributes of a [ModHashMap].
     *
     * @param mods The [ModHashMap] to retrieve.
     * @param mode The [GameMode] to get for.
     * @param cacheMap The map containing the cache to lookup for.
     * @return The difficulty attributes, `null` if not found.
     * @param <T> The difficulty attributes cache type.
    </T> */
    private fun <T> getCache(
        mods: Iterable<Mod>?, mode: GameMode,
        cacheMap: HashMap<Set<Mod>, BeatmapDifficultyCache<T>>
    ) = cacheMap[processMods(mods, mode)]?.let {
        it.refresh()
        it.cache
    }

    /**
     * Processes and copies a set of [Mod]s for caching.
     *
     * @param mods The [Mod]s to process.
     * @param mode The [GameMode] to process for.
     * @return A new set of [Mod]s that can be used as a cache.
     */
    private fun processMods(mods: Iterable<Mod>?, mode: GameMode) = when (mode) {
        GameMode.Droid -> droidDifficultyCalculator.retainDifficultyAdjustmentMods(mods)
        GameMode.Standard -> standardDifficultyCalculator.retainDifficultyAdjustmentMods(mods)
    }
}

/**
 * Represents a beatmap difficulty cache.
 */
private class BeatmapDifficultyCache<T>(
    /**
     * The cached data.
     */
    val cache: T,

    /**
     * The duration at which this cache is allowed to live, in milliseconds.
     */
    val timeToLive: Long
) {
    /**
     * The time at which this cache was last accessed, in milliseconds.
     */
    var lastAccessedTime = System.currentTimeMillis()
        private set

    /**
     * Refreshes the cache.
     */
    fun refresh() {
        lastAccessedTime = System.currentTimeMillis()
    }

    /**
     * Determines whether this cache has expired.
     *
     * @param time The time to test against, in milliseconds.
     * @return Whether the cache has expired.
     */
    fun isExpired(time: Long) = lastAccessedTime + timeToLive < time
}

operator fun <K : Any, V : Any> MutableMap<K, V>.get(
    key: K,
    fallback: () -> V
) = this[key] ?: fallback().also { this[key] = it }
