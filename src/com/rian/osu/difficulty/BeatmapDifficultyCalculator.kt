package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.IBeatmap
import com.rian.osu.beatmap.PlayableBeatmap
import com.rian.osu.beatmap.StandardPlayableBeatmap
import com.rian.osu.difficulty.attributes.*
import com.rian.osu.difficulty.calculator.*
import com.rian.osu.replay.SliderCheeseChecker
import com.rian.osu.replay.ThreeFingerChecker
import com.rian.osu.replay.createCursorGroups
import com.rian.osu.utils.LRUCache
import com.rian.osu.utils.ModUtils
import ru.nsu.ccfit.zuev.osu.scoring.Replay.MoveArray
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope

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
     * Constructs a [DifficultyCalculationParameters] from a [StatisticV2].
     *
     * @param stat The [StatisticV2] to construct the [DifficultyCalculationParameters] from.
     * @return The [DifficultyCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructDifficultyParameters(stat: StatisticV2?) = stat?.run {
        DifficultyCalculationParameters().also {
            it.mods = ModUtils.convertLegacyMods(
                mod,
                if (isCustomCS) customCS else null,
                if (isCustomAR) customAR else null,
                if (isCustomOD) customOD else null,
                if (isCustomHP) customHP else null
            )

            it.customSpeedMultiplier = changeSpeed
        }
    }

    /**
     * Constructs a [DifficultyCalculationParameters] from a [PlayableBeatmap].
     *
     * @param beatmap The [PlayableBeatmap] to construct the [DifficultyCalculationParameters] from.
     * @return The [DifficultyCalculationParameters] representing the [PlayableBeatmap].
     */
    @JvmStatic
    fun constructDifficultyParameters(beatmap: PlayableBeatmap) = DifficultyCalculationParameters().also {
        it.mods = beatmap.mods?.toMutableList() ?: mutableListOf()
        it.customSpeedMultiplier = beatmap.customSpeedMultiplier
    }

    /**
     * Constructs a [DroidPerformanceCalculationParameters] from a [StatisticV2].
     *
     * @param stat The [StatisticV2] to construct the [DroidPerformanceCalculationParameters] from.
     * @return The [DroidPerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructDroidPerformanceParameters(stat: StatisticV2?) = stat?.run {
        DroidPerformanceCalculationParameters().also {
            it.maxCombo = getScoreMaxCombo()
            it.countGreat = hit300
            it.countOk = hit100
            it.countMeh = hit50
            it.countMiss = misses
        }
    }

    /**
     * Constructs a [PerformanceCalculationParameters] from a [StatisticV2].
     *
     * @param stat The [StatisticV2] to construct the [PerformanceCalculationParameters] from.
     * @return The [PerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructStandardPerformanceParameters(stat: StatisticV2?) = stat?.run {
        PerformanceCalculationParameters().also {
            it.maxCombo = getScoreMaxCombo()
            it.countGreat = hit300
            it.countOk = hit100
            it.countMeh = hit50
            it.countMiss = misses
        }
    }

    /**
     * Calculates the osu!droid difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param stat The [StatisticV2] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [Beatmap] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficulty(beatmap: Beatmap, stat: StatisticV2, scope: CoroutineScope? = null) =
        calculateDroidDifficulty(beatmap, constructDifficultyParameters(stat), scope)

    /**
     * Calculates the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(parameters) ?:
        droidDifficultyCalculator.calculate(beatmap, parameters, scope).also { addCache(beatmap, parameters, it) }

    /**
     * Calculates the difficulty of a [DroidPlayableBeatmap].
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [DroidPlayableBeatmap].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficulty(beatmap: DroidPlayableBeatmap, scope: CoroutineScope? = null) = run {
        val parameters = constructDifficultyParameters(beatmap)

        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(parameters) ?:
        droidDifficultyCalculator.calculate(beatmap, scope).also { addCache(beatmap, parameters, it) }
    }

    /**
     * Calculates the difficulty of a [Beatmap], returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [Beatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of
     * the [Beatmap] at any relevant time relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidTimedDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidTimedDifficultyCache(parameters) ?:
        droidDifficultyCalculator.calculateTimed(beatmap, parameters, scope).also { addCache(beatmap, parameters, it) }

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
    fun calculateDroidTimedDifficulty(beatmap: DroidPlayableBeatmap, scope: CoroutineScope? = null) = run {
        val parameters = constructDifficultyParameters(beatmap)

        difficultyCacheManager[beatmap.md5]?.getDroidTimedDifficultyCache(parameters) ?:
        droidDifficultyCalculator.calculateTimed(beatmap, scope).also { addCache(beatmap, parameters, it) }
    }

    /**
     * Calculates the osu!standard difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!standard difficulty of the [Beatmap] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardDifficulty(beatmap: Beatmap, stat: StatisticV2, scope: CoroutineScope? = null) =
        calculateStandardDifficulty(beatmap, constructDifficultyParameters(stat), scope)

    /**
     * Calculates the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the osu!standard difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardDifficultyCache(parameters) ?:
        standardDifficultyCalculator.calculate(beatmap, parameters, scope).also { addCache(beatmap, parameters, it) }

    /**
     * Calculates the difficulty of a [StandardPlayableBeatmap].
     *
     * @param beatmap The [StandardPlayableBeatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!standard difficulty of the [StandardPlayableBeatmap].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardDifficulty(beatmap: StandardPlayableBeatmap, scope: CoroutineScope? = null) = run {
        val parameters = constructDifficultyParameters(beatmap)

        difficultyCacheManager[beatmap.md5]?.getStandardDifficultyCache(parameters) ?:
        standardDifficultyCalculator.calculate(beatmap, scope).also { addCache(beatmap, parameters, it) }
    }

    /**
     * Calculates the difficulty of a [Beatmap], returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [Beatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of
     * the [Beatmap] at any relevant time relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardTimedDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardTimedDifficultyCache(parameters) ?:
        standardDifficultyCalculator.calculateTimed(beatmap, parameters, scope).also { addCache(beatmap, parameters, it) }

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
    fun calculateStandardTimedDifficulty(beatmap: StandardPlayableBeatmap, scope: CoroutineScope? = null) = run {
        val parameters = constructDifficultyParameters(beatmap)

        difficultyCacheManager[beatmap.md5]?.getStandardTimedDifficultyCache(parameters) ?:
        standardDifficultyCalculator.calculateTimed(beatmap, scope).also { addCache(beatmap, parameters, it) }
    }

    /**
     * Calculates the performance of a [DroidDifficultyAttributes].
     *
     * @param attributes The [DroidDifficultyAttributes] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateDroidPerformance(attributes: DroidDifficultyAttributes, stat: StatisticV2) =
        calculateDroidPerformance(attributes, constructDroidPerformanceParameters(stat))

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
    ) = calculateDroidPerformance(beatmap, attributes, replayMovements, replayObjectData, constructDroidPerformanceParameters(stat))

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
            beatmap.createDroidPlayableBeatmap(attributes.mods, attributes.customSpeedMultiplier),
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
    ) = calculateDroidPerformance(beatmap, attributes, replayMovements, replayObjectData, constructDroidPerformanceParameters(stat))

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
            }

        return DroidPerformanceCalculator(attributes).calculate(actualParameters)
    }

    /**
     * Calculates the performance of a [StandardDifficultyAttributes].
     *
     * @param attributes The [StandardDifficultyAttributes] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [StandardDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateStandardPerformance(attributes: StandardDifficultyAttributes, stat: StatisticV2) =
        calculateStandardPerformance(attributes, constructStandardPerformanceParameters(stat))

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
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(
        beatmap: IBeatmap, parameters: DifficultyCalculationParameters?,
        attributes: DroidDifficultyAttributes
    ) = difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, 60 * 1000) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(
        beatmap: IBeatmap, parameters: DifficultyCalculationParameters?,
        attributes: StandardDifficultyAttributes
    ) = difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, 60 * 1000) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addDroidTimedCache")
    private fun addCache(
        beatmap: IBeatmap, parameters: DifficultyCalculationParameters?,
        attributes: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>
    ) =
        // Allow a maximum of 5 minutes of living cache.
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, min(
            beatmap.duration.toLong(),
            5 * 60 * 1000
        )) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addStandardTimedCache")
    private fun addCache(
        beatmap: IBeatmap, parameters: DifficultyCalculationParameters?,
        attributes: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>
    ) =
        // Allow a maximum of 5 minutes of living cache.
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, min(
            beatmap.duration.toLong(),
            5 * 60 * 1000
        )) }
}

/**
 * A cache holder for a [Beatmap].
 */
private class BeatmapDifficultyCacheManager {
    private val droidAttributeCache =
        LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<DroidDifficultyAttributes>>(5)
    private val droidTimedAttributeCache =
        LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>>>(3)
    private val standardAttributeCache =
        LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<StandardDifficultyAttributes>>(5)
    private val standardTimedAttributeCache =
        LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>>>(3)

    /**
     * Adds a [DroidDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param parameters The [DifficultyCalculationParameters] of the [DroidDifficultyAttributes].
     * @param attributes The [DroidDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    fun addCache(parameters: DifficultyCalculationParameters?, attributes: DroidDifficultyAttributes, timeToLive: Long) =
        addCache(parameters, GameMode.Droid, attributes, droidAttributeCache, timeToLive)

    /**
     * Adds a [StandardDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param parameters The [DifficultyCalculationParameters] of the [StandardDifficultyAttributes].
     * @param attributes The [StandardDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    fun addCache(parameters: DifficultyCalculationParameters?, attributes: StandardDifficultyAttributes, timeToLive: Long) =
        addCache(parameters, GameMode.Standard, attributes, standardAttributeCache, timeToLive)

    /**
     * Adds a [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param parameters The [DifficultyCalculationParameters] of the difficulty attributes.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    @JvmName("addDroidTimedCache")
    fun addCache(
        parameters: DifficultyCalculationParameters?,
        attributes: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>,
        timeToLive: Long
    ) = addCache(parameters, GameMode.Droid, attributes, droidTimedAttributeCache, timeToLive)

    /**
     * Adds a [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param parameters The [DifficultyCalculationParameters] of the difficulty attributes.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    @JvmName("addStandardTimedCache")
    fun addCache(
        parameters: DifficultyCalculationParameters?,
        attributes: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>,
        timeToLive: Long
    ) = addCache(parameters, GameMode.Standard, attributes, standardTimedAttributeCache, timeToLive)

    /**
     * Retrieves the [DroidDifficultyAttributes] cache of a [DifficultyCalculationParameters].
     *
     * @param parameters The [DifficultyCalculationParameters] to retrieve.
     * @return The [DroidDifficultyAttributes], `null` if not found.
     */
    fun getDroidDifficultyCache(parameters: DifficultyCalculationParameters?) =
        getCache(parameters, GameMode.Droid, droidAttributeCache)

    /**
     * Retrieves the [TimedDifficultyAttributes] cache of a [DifficultyCalculationParameters].
     *
     * @param parameters The [DifficultyCalculationParameters] to retrieve.
     * @return The [TimedDifficultyAttributes], `null` if not found.
     */
    fun getDroidTimedDifficultyCache(parameters: DifficultyCalculationParameters?) =
        getCache(parameters, GameMode.Droid, droidTimedAttributeCache)

    /**
     * Retrieves the [StandardDifficultyAttributes] cache of a [DifficultyCalculationParameters].
     *
     * @param parameters The [DifficultyCalculationParameters] to retrieve.
     * @return The [StandardDifficultyAttributes], `null` if not found.
     */
    fun getStandardDifficultyCache(parameters: DifficultyCalculationParameters?) =
        getCache(parameters, GameMode.Standard, standardAttributeCache)

    /**
     * Retrieves the [TimedDifficultyAttributes] cache of a [DifficultyCalculationParameters].
     *
     * @param parameters The [DifficultyCalculationParameters] to retrieve.
     * @return The [TimedDifficultyAttributes], `null` if not found.
     */
    fun getStandardTimedDifficultyCache(parameters: DifficultyCalculationParameters?) =
        getCache(parameters, GameMode.Standard, standardTimedAttributeCache)

    /**
     * Whether this [BeatmapDifficultyCacheManager] does not hold any cache.
     */
    val isEmpty: Boolean
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
        cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>
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
     * @param parameters The difficulty calculation parameter to cache.
     * @param mode The [GameMode] to get for.
     * @param cache The difficulty attributes cache to add.
     * @param cacheMap The map to add the cache to.
     * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
     */
    private fun <T> addCache(
        parameters: DifficultyCalculationParameters?, mode: GameMode, cache: T,
        cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>,
        timeToLive: Long
    ) {
        cacheMap[processParameters(parameters, mode)] = BeatmapDifficultyCache(cache, timeToLive)
    }

    /**
     * Gets the cache of difficulty attributes of a calculation parameter.
     *
     * @param parameters The difficulty calculation parameter to retrieve.
     * @param mode The [GameMode] to get for.
     * @param cacheMap The map containing the cache to lookup for.
     * @return The difficulty attributes, `null` if not found.
     */
    private fun <T> getCache(
        parameters: DifficultyCalculationParameters?, mode: GameMode,
        cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>
    ) = cacheMap[processParameters(parameters, mode)]?.let {
        it.refresh()
        it.cache
    }

    /**
     * Processes and copies a [DifficultyCalculationParameters] for caching.
     *
     * @param parameters The [DifficultyCalculationParameters] to process.
     * @param mode The [GameMode] to process for.
     * @return A new [DifficultyCalculationParameters] that can be used as a cache.
     */
    private fun processParameters(parameters: DifficultyCalculationParameters?, mode: GameMode) =
        parameters?.copy()?.also {
            // Copy the parameter for caching.
            when (mode) {
                GameMode.Droid -> droidDifficultyCalculator.retainDifficultyAdjustmentMods(it)
                GameMode.Standard -> standardDifficultyCalculator.retainDifficultyAdjustmentMods(it)
            }
        } ?: DifficultyCalculationParameters()
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
