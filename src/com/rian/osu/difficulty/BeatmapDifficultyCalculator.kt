package com.rian.osu.difficulty

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.difficulty.attributes.DifficultyAttributes
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes
import com.rian.osu.difficulty.calculator.*
import com.rian.osu.utils.convertLegacyMods
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.collections.Map.Entry
import kotlin.math.ceil
import kotlin.math.min

/**
 * A helper class for operations relating to difficulty and performance calculation.
 */
object BeatmapDifficultyCalculator {
    /**
     * Cache of difficulty calculations, mapped by MD5 hash of a beatmap.
     */
    private val difficultyCacheManager = LRUCache<String, BeatmapDifficultyCacheManager>(10)

    private val droidDifficultyCalculator = DroidDifficultyCalculator()
    private val standardDifficultyCalculator = StandardDifficultyCalculator()

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
            it.mods = convertLegacyMods(
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
     * Constructs a [DroidPerformanceCalculationParameters] from a [StatisticV2].
     *
     * @param stat The [StatisticV2] to construct the [DroidPerformanceCalculationParameters] from.
     * @return The [DroidPerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructDroidPerformanceParameters(stat: StatisticV2?) = stat?.run {
        DroidPerformanceCalculationParameters().also {
            it.maxCombo = getMaxCombo()
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
            it.maxCombo = getMaxCombo()
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
     * @return A structure describing the osu!droid difficulty of the [Beatmap] relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateDroidDifficulty(beatmap: Beatmap, stat: StatisticV2) =
        calculateDroidDifficulty(beatmap, constructDifficultyParameters(stat))

    /**
     * Calculates the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the osu!droid difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDroidDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidDifficultyCache(parameters) ?:
        droidDifficultyCalculator.calculate(beatmap, parameters).also { addCache(beatmap, parameters, it) }

    /**
     * Calculates the difficulty of a [Beatmap], returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [Beatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of
     * the [Beatmap] at any relevant time relating to the calculation parameters.
     */
    @JvmStatic
    fun calculateDroidTimedDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null) =
        difficultyCacheManager[beatmap.md5]?.getDroidTimedDifficultyCache(parameters) ?:
        droidDifficultyCalculator.calculateTimed(beatmap, parameters).also { addCache(beatmap, parameters, it) }

    /**
     * Calculates the osu!standard difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the osu!standard difficulty of the [Beatmap] relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateStandardDifficulty(beatmap: Beatmap, stat: StatisticV2) =
        calculateStandardDifficulty(beatmap, constructDifficultyParameters(stat))

    /**
     * Calculates the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the osu!standard difficulty of the [Beatmap] relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardDifficultyCache(parameters) ?:
        standardDifficultyCalculator.calculate(beatmap, parameters).also { addCache(beatmap, parameters, it) }

    /**
     * Calculates the difficulty of a [Beatmap], returning a set of [TimedDifficultyAttributes]
     * representing the difficulty of the [Beatmap] at any relevant time.
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A set of [TimedDifficultyAttributes] describing the difficulty of
     * the [Beatmap] at any relevant time relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateStandardTimedDifficulty(beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null) =
        difficultyCacheManager[beatmap.md5]?.getStandardTimedDifficultyCache(parameters) ?:
        standardDifficultyCalculator.calculateTimed(beatmap, parameters).also { addCache(beatmap, parameters, it) }

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
     * @param beatmap The [Beatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?,
        attributes: DroidDifficultyAttributes
    ) = difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, 60 * 1000) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [Beatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [DifficultyAttributes] to cache.
     */
    private fun addCache(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?,
        attributes: StandardDifficultyAttributes
    ) = difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, 60 * 1000) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [Beatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addDroidTimedCache")
    private fun addCache(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?,
        attributes: List<TimedDifficultyAttributes<DroidDifficultyAttributes>>
    ) =
        // Allow a maximum of 5 minutes of living cache.
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, min(
            beatmap.duration.toLong(),
            5 * 60 * 1000
        )) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [Beatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    @JvmName("addStandardTimedCache")
    private fun addCache(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?,
        attributes: List<TimedDifficultyAttributes<StandardDifficultyAttributes>>
    ) =
        // Allow a maximum of 5 minutes of living cache.
        difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, min(
            beatmap.duration.toLong(),
            5 * 60 * 1000
        )) }

    /**
     * An implementation of least-recently-used cache using [LinkedHashMap]s.
     *
     * @param <K> The key of the cache.
     * @param <V> The value to cache.
     */
    private class LRUCache<K, V>(
        private val maxSize: Int
    ) : LinkedHashMap<K, V>(ceil((maxSize / 0.75f).toDouble()).toInt(), 0.75f, true) {

        override fun removeEldestEntry(eldest: Entry<K, V>) = size > maxSize
    }

    /**
     * A cache holder for a [Beatmap].
     */
    private class BeatmapDifficultyCacheManager {
        private val droidAttributeCache =
            LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<DroidDifficultyAttributes>>(5)
        private val droidTimedAttributeCache =
            LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<List<TimedDifficultyAttributes<DroidDifficultyAttributes>>>>(3)
        private val standardAttributeCache =
            LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<StandardDifficultyAttributes>>(5)
        private val standardTimedAttributeCache =
            LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<List<TimedDifficultyAttributes<StandardDifficultyAttributes>>>>(3)

        /**
         * Adds a [DroidDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
         *
         * @param parameters The [DifficultyCalculationParameters] of the [DroidDifficultyAttributes].
         * @param attributes The [DroidDifficultyAttributes] to cache.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         */
        fun addCache(
            parameters: DifficultyCalculationParameters?, attributes: DroidDifficultyAttributes,
            timeToLive: Long
        ) = addCache(parameters, attributes, droidAttributeCache, timeToLive)

        /**
         * Adds a [StandardDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
         *
         * @param parameters The [DifficultyCalculationParameters] of the [StandardDifficultyAttributes].
         * @param attributes The [StandardDifficultyAttributes] to cache.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         */
        fun addCache(
            parameters: DifficultyCalculationParameters?, attributes: StandardDifficultyAttributes,
            timeToLive: Long
        ) = addCache(parameters, attributes, standardAttributeCache, timeToLive)

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
            attributes: List<TimedDifficultyAttributes<DroidDifficultyAttributes>>,
            timeToLive: Long
        ) = addCache(parameters, attributes, droidTimedAttributeCache, timeToLive)

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
            attributes: List<TimedDifficultyAttributes<StandardDifficultyAttributes>>,
            timeToLive: Long
        ) = addCache(parameters, attributes, standardTimedAttributeCache, timeToLive)

        /**
         * Retrieves the [DroidDifficultyAttributes] cache of a [DifficultyCalculationParameters].
         *
         * @param parameters The [DifficultyCalculationParameters] to retrieve.
         * @return The [DroidDifficultyAttributes], `null` if not found.
         */
        fun getDroidDifficultyCache(parameters: DifficultyCalculationParameters?) =
            getCache(parameters, droidAttributeCache)

        /**
         * Retrieves the [TimedDifficultyAttributes] cache of a [DifficultyCalculationParameters].
         *
         * @param parameters The [DifficultyCalculationParameters] to retrieve.
         * @return The [TimedDifficultyAttributes], `null` if not found.
         */
        fun getDroidTimedDifficultyCache(parameters: DifficultyCalculationParameters?) =
            getCache(parameters, droidTimedAttributeCache)

        /**
         * Retrieves the [StandardDifficultyAttributes] cache of a [DifficultyCalculationParameters].
         *
         * @param parameters The [DifficultyCalculationParameters] to retrieve.
         * @return The [StandardDifficultyAttributes], `null` if not found.
         */
        fun getStandardDifficultyCache(parameters: DifficultyCalculationParameters?) =
            getCache(parameters, standardAttributeCache)

        /**
         * Retrieves the [TimedDifficultyAttributes] cache of a [DifficultyCalculationParameters].
         *
         * @param parameters The [DifficultyCalculationParameters] to retrieve.
         * @return The [TimedDifficultyAttributes], `null` if not found.
         */
        fun getStandardTimedDifficultyCache(parameters: DifficultyCalculationParameters?) =
            getCache(parameters, standardTimedAttributeCache)

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
         * @param cache The difficulty attributes cache to add.
         * @param cacheMap The map to add the cache to.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         * @param <T> The difficulty attributes cache type.
        </T> */
        private fun <T> addCache(
            parameters: DifficultyCalculationParameters?, cache: T,
            cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>,
            timeToLive: Long
        ) {
            cacheMap[processParameters(parameters)] = BeatmapDifficultyCache(cache, timeToLive)
        }

        /**
         * Gets the cache of difficulty attributes of a calculation parameter.
         *
         * @param parameters The difficulty calculation parameter to retrieve.
         * @param cacheMap The map containing the cache to lookup for.
         * @return The difficulty attributes, `null` if not found.
         * @param <T> The difficulty attributes cache type.
        </T> */
        private fun <T> getCache(
            parameters: DifficultyCalculationParameters?,
            cacheMap: HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>>
        ) = cacheMap[processParameters(parameters)]?.cache

        /**
         * Processes and copies a [DifficultyCalculationParameters] for caching.
         *
         * @param parameters The [DifficultyCalculationParameters] to process.
         * @return A new [DifficultyCalculationParameters] that can be used as a cache.
         */
        private fun processParameters(parameters: DifficultyCalculationParameters?) =
            parameters?.copy()?.also {
                // Copy the parameter for caching.
                DifficultyCalculator.retainDifficultyAdjustmentMods(it)
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
         * The time at which this cache was generated, in milliseconds.
         */
        val generatedTime = System.currentTimeMillis()

        /**
         * Determines whether this cache has expired.
         *
         * @param time The time to test against, in milliseconds.
         * @return Whether the cache has expired.
         */
        fun isExpired(time: Long) = generatedTime + timeToLive < time
    }
}

operator fun <K : Any, V : Any> MutableMap<K, V>.get(
    key: K,
    fallback: () -> V
) = this[key] ?: fallback().also { this[key] = it }
