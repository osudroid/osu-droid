package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.IBeatmap
import com.rian.osu.beatmap.StandardPlayableBeatmap
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
import kotlinx.coroutines.CoroutineScope
import ru.nsu.ccfit.zuev.osu.scoring.Replay

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
            it.populate(beatmap, this)
        }
    }

    /**
     * Constructs a [StandardPerformanceCalculationParameters] from an [IBeatmap] and [StatisticV2].
     *
     * @param beatmap The [IBeatmap] to construct the [StandardPerformanceCalculationParameters] from.
     * @param stat The [StatisticV2] to construct the [StandardPerformanceCalculationParameters] from.
     * @return The [StandardPerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructStandardPerformanceParameters(beatmap: IBeatmap, stat: StatisticV2?) = stat?.run {
        StandardPerformanceCalculationParameters().also {
            it.populate(beatmap, this)
        }
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
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(mods, false) ?:
        droidDifficultyCalculator.calculate(beatmap, mods, scope).also { addCache(beatmap, it, false) }

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
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(beatmap.mods.values, false) ?:
        droidDifficultyCalculator.calculate(beatmap, scope).also { addCache(beatmap, it, false) }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s. The result of this calculation can be used in
     * replay-based performance calculations.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficultyForReplay(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(mods, true) ?:
        droidDifficultyCalculator.calculate(beatmap, mods, scope).also { addCache(beatmap, it, true) }

    /**
     * Calculates the difficulty of a [DroidPlayableBeatmap]. The result of this calculation can be used in
     * replay-based performance calculations.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the osu!droid difficulty of the [DroidPlayableBeatmap].
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficultyForReplay(beatmap: DroidPlayableBeatmap, scope: CoroutineScope? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(beatmap.mods.values, true) ?:
        droidDifficultyCalculator.calculate(beatmap, scope).also { addCache(beatmap, it, true) }

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
     * @param replay The [Replay] to calculate for.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithStat")
    fun calculateDroidPerformance(
        beatmap: Beatmap,
        attributes: DroidDifficultyAttributes,
        replay: Replay,
        stat: StatisticV2? = null
    ) = calculateDroidPerformance(beatmap, attributes, replay, constructDroidPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [Beatmap].
     * @param replay The [Replay] to calculate for.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithParameters")
    fun calculateDroidPerformance(
        beatmap: Beatmap,
        attributes: DroidDifficultyAttributes,
        replay: Replay,
        parameters: DroidPerformanceCalculationParameters? = null
    ) = calculateDroidPerformance(
            beatmap.createDroidPlayableBeatmap(attributes.mods),
            attributes, replay, parameters
        )

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [DroidPlayableBeatmap].
     * @param replay The [Replay] to calculate for.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the [StatisticV2].
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithReplayStat")
    fun calculateDroidPerformance(
        beatmap: DroidPlayableBeatmap,
        attributes: DroidDifficultyAttributes,
        replay: Replay,
        stat: StatisticV2? = null
    ) = calculateDroidPerformance(beatmap, attributes, replay, constructDroidPerformanceParameters(beatmap, stat))

    /**
     * Calculates the performance of a [DroidDifficultyAttributes] and applies necessary adjustments to
     * the performance value using replay data.
     *
     * @param beatmap The [DroidPlayableBeatmap] to calculate.
     * @param attributes The [DroidDifficultyAttributes] of the [DroidPlayableBeatmap].
     * @param replay The [Replay] to calculate for.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DroidDifficultyAttributes] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateDroidPerformanceWithReplayParameters")
    fun calculateDroidPerformance(
        beatmap: DroidPlayableBeatmap,
        attributes: DroidDifficultyAttributes,
        replay: Replay,
        parameters: DroidPerformanceCalculationParameters? = null
    ): DroidPerformanceAttributes {
        val actualParameters =
            (parameters ?: DroidPerformanceCalculationParameters()).also {
                val cursorGroups = createCursorGroups(replay.cursorMoves)

                it.tapPenalty = ThreeFingerChecker(
                    beatmap, attributes, cursorGroups, replay.objectData
                ).calculatePenalty()

                it.sliderCheesePenalty = SliderCheeseChecker(
                    beatmap, attributes, replay.replayVersion, cursorGroups, replay.objectData
                ).calculatePenalty()

                it.populateNestedSliderObjectParameters(beatmap, replay.objectData)
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
        parameters: StandardPerformanceCalculationParameters? = null
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
        parameters: StandardPerformanceCalculationParameters? = null
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
        parameters: StandardPerformanceCalculationParameters? = null
    ): StandardPerformanceAttributes {
        val actualParameters =
            (parameters ?: StandardPerformanceCalculationParameters()).also {
                it.populateNestedSliderObjectParameters(beatmap, replayObjectData)
            }

        return StandardPerformanceCalculator(attributes).calculate(actualParameters)
    }

    /**
     * Clears all entries from the difficulty cache.
     */
    @JvmStatic
    fun clearCache() = difficultyCacheManager.clear()

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     * @param forReplay Whether this cache is for replay-based calculations.
     */
    private fun addCache(beatmap: IBeatmap, attributes: DroidDifficultyAttributes, forReplay: Boolean) =
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes, forReplay) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [IBeatmap] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(beatmap: IBeatmap, attributes: StandardDifficultyAttributes) =
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes) }

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
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes) }

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
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(attributes) }
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
     * @param forReplay Whether this cache is for replay-based calculations.
     */
    fun addCache(attributes: DroidDifficultyAttributes, forReplay: Boolean) =
        addCache(attributes.mods, GameMode.Droid, attributes, droidAttributeCache, forReplay)

    /**
     * Adds a [StandardDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The [StandardDifficultyAttributes] to cache.
     */
    fun addCache(attributes: StandardDifficultyAttributes) =
        addCache(attributes.mods, GameMode.Standard, attributes, standardAttributeCache, false)

    /**
     * Adds a set of [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The set of [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addDroidTimedCache")
    fun addCache(attributes: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>) =
        addCache(attributes.first().attributes.mods, GameMode.Droid, attributes, droidTimedAttributeCache, false)

    /**
     * Adds a set of [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
     *
     * @param attributes The set of [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addStandardTimedCache")
    fun addCache(attributes: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>) =
        addCache(attributes.first().attributes.mods, GameMode.Standard, attributes, standardTimedAttributeCache, false)

    /**
     * Retrieves the [DroidDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @param forReplay Whether to retrieve a cache for replay-based calculations.
     * @return The [DroidDifficultyAttributes], `null` if not found.
     */
    fun getDroidDifficultyCache(mods: Iterable<Mod>?, forReplay: Boolean) = getCache(mods, GameMode.Droid, droidAttributeCache, forReplay)

    /**
     * Retrieves the [TimedDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [TimedDifficultyAttributes], `null` if not found.
     */
    fun getDroidTimedDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Droid, droidTimedAttributeCache, false)

    /**
     * Retrieves the [StandardDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [StandardDifficultyAttributes], `null` if not found.
     */
    fun getStandardDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Standard, standardAttributeCache, false)

    /**
     * Retrieves the [TimedDifficultyAttributes] cache of a set of [Mod]s.
     *
     * @param mods The [Mod]s to retrieve.
     * @return The [TimedDifficultyAttributes], `null` if not found.
     */
    fun getStandardTimedDifficultyCache(mods: Iterable<Mod>?) = getCache(mods, GameMode.Standard, standardTimedAttributeCache, false)

    /**
     * Whether this [BeatmapDifficultyCacheManager] does not hold any cache.
     */
    val isEmpty
        get() = droidAttributeCache.isEmpty() && droidTimedAttributeCache.isEmpty() &&
                standardAttributeCache.isEmpty() && standardTimedAttributeCache.isEmpty()

    /**
     * Adds a difficulty attributes cache to a cache map.
     *
     * @param mods The [ModHashMap] to cache for.
     * @param mode The [GameMode] to get for.
     * @param cache The difficulty attributes cache to add.
     * @param cacheMap The map to add the cache to.
     * @param forReplay Whether this cache is for replay-based calculations.
     */
    private fun <T> addCache(
        mods: Iterable<Mod>?, mode: GameMode, cache: T,
        cacheMap: HashMap<Set<Mod>, BeatmapDifficultyCache<T>>,
        forReplay: Boolean
    ) {
        val existing = cacheMap[processMods(mods, mode)]

        if (!forReplay && existing != null && existing.forReplay) {
            // Do not overwrite a replay cache with a non-replay cache.
            return
        }

        cacheMap[processMods(mods, mode)] = BeatmapDifficultyCache(cache, forReplay)
    }

    /**
     * Gets the cache of difficulty attributes of a [ModHashMap].
     *
     * @param T The type of difficulty attributes to retrieve.
     * @param mods The [ModHashMap] to retrieve.
     * @param mode The [GameMode] to get for.
     * @param cacheMap The map containing the cache to lookup for.
     * @param forReplay Whether to retrieve a cache for replay-based calculations.
     * @return The difficulty attributes, `null` if not found.
     */
    private fun <T> getCache(
        mods: Iterable<Mod>?,
        mode: GameMode,
        cacheMap: HashMap<Set<Mod>, BeatmapDifficultyCache<T>>,
        forReplay: Boolean
    ): T? {
        val cache = cacheMap[processMods(mods, mode)]

        if (forReplay && cache?.forReplay != true) {
            return null
        }

        return cache?.cache
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
private data class BeatmapDifficultyCache<T>(
    /**
     * The cached data.
     */
    val cache: T,

    /**
     * Whether this cache is for replay-based calculations.
     */
    val forReplay: Boolean
)

operator fun <K : Any, V : Any> MutableMap<K, V>.get(
    key: K,
    fallback: () -> V
) = this[key] ?: fallback().also { this[key] = it }
