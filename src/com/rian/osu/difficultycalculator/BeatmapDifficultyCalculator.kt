package com.rian.osu.difficultycalculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.difficultycalculator.attributes.DifficultyAttributes
import com.rian.osu.difficultycalculator.attributes.TimedDifficultyAttributes
import com.rian.osu.difficultycalculator.calculator.DifficultyCalculationParameters
import com.rian.osu.difficultycalculator.calculator.DifficultyCalculator
import com.rian.osu.difficultycalculator.calculator.PerformanceCalculationParameters
import com.rian.osu.difficultycalculator.calculator.PerformanceCalculator
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
     * Constructs a [PerformanceCalculationParameters] from a [StatisticV2].
     *
     * @param stat The [StatisticV2] to construct the [PerformanceCalculationParameters] from.
     * @return The [PerformanceCalculationParameters] representing the [StatisticV2],
     * `null` if the [StatisticV2] instance is `null`.
     */
    @JvmStatic
    fun constructPerformanceParameters(stat: StatisticV2?) = stat?.run {
        PerformanceCalculationParameters().also {
            it.maxCombo = getMaxCombo()
            it.countGreat = hit300
            it.countOk = hit100
            it.countMeh = hit50
            it.countMiss = misses
        }
    }

    /**
     * Calculates the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the difficulty of the [Beatmap]
     * relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculateDifficulty(beatmap: Beatmap, stat: StatisticV2) =
        calculateDifficulty(beatmap, constructDifficultyParameters(stat))

    /**
     * Calculates the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the difficulty of the [Beatmap]
     * relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateDifficulty(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null
    ) = difficultyCacheManager[beatmap.md5]?.getDifficultyCache(parameters)
        ?:
        DifficultyCalculator.calculate(beatmap, parameters).also { addCache(beatmap, parameters, it) }

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
    fun calculateTimedDifficulty(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters? = null
    ) = difficultyCacheManager[beatmap.md5]?.getTimedDifficultyCache(parameters)
        ?:
        DifficultyCalculator.calculateTimed(beatmap, parameters).also { addCache(beatmap, parameters, it) }

    /**
     * Calculates the performance of a [DifficultyAttributes].
     *
     * @param attributes The [DifficultyAttributes] to calculate.
     * @param stat The [StatisticV2] to calculate for.
     * @return A structure describing the performance of the [DifficultyAttributes]
     * relating to the [StatisticV2].
     */
    @JvmStatic
    fun calculatePerformance(attributes: DifficultyAttributes, stat: StatisticV2) =
        calculatePerformance(attributes, constructPerformanceParameters(stat))

    /**
     * Calculates the performance of a [DifficultyAttributes].
     *
     * @param attributes The [DifficultyAttributes] to calculate.
     * @param parameters The parameters of the calculation. Can be `null`.
     * @return A structure describing the performance of the [DifficultyAttributes]
     * relating to the calculation parameters.
     */
    @JvmStatic
    @JvmOverloads
    fun calculatePerformance(
        attributes: DifficultyAttributes,
        parameters: PerformanceCalculationParameters? = null
    ) = PerformanceCalculator(attributes).calculate(parameters)

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
        attributes: DifficultyAttributes
    ) = difficultyCacheManager[beatmap.md5, { BeatmapDifficultyCacheManager() }].run { addCache(parameters, attributes, 60 * 1000) }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The [Beatmap] to cache.
     * @param parameters The [DifficultyCalculationParameters] to cache.
     * @param attributes The [TimedDifficultyAttributes] to cache.
     */
    private fun addCache(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?,
        attributes: List<TimedDifficultyAttributes>
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
        private val attributeCache =
            LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<DifficultyAttributes>>(5)
        private val timedAttributeCache =
            LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<List<TimedDifficultyAttributes>>>(3)

        /**
         * Adds a [DifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
         *
         * @param parameters The [DifficultyCalculationParameters] of the [DifficultyAttributes].
         * @param attributes The [DifficultyAttributes] to cache.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         */
        fun addCache(
            parameters: DifficultyCalculationParameters?, attributes: DifficultyAttributes,
            timeToLive: Long
        ) = addCache(parameters, attributes, attributeCache, timeToLive)

        /**
         * Adds a [TimedDifficultyAttributes] cache to this [BeatmapDifficultyCacheManager].
         *
         * @param parameters The [DifficultyCalculationParameters] of the difficulty attributes.
         * @param attributes The [TimedDifficultyAttributes] to cache.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         */
        fun addCache(
            parameters: DifficultyCalculationParameters?, attributes: List<TimedDifficultyAttributes>,
            timeToLive: Long
        ) = addCache(parameters, attributes, timedAttributeCache, timeToLive)

        /**
         * Retrieves the [DifficultyAttributes] cache of a [DifficultyCalculationParameters].
         *
         * @param parameters The [DifficultyCalculationParameters] to retrieve.
         * @return The [DifficultyAttributes], `null` if not found.
         */
        fun getDifficultyCache(parameters: DifficultyCalculationParameters?) =
            getCache(parameters, attributeCache)

        /**
         * Retrieves the [TimedDifficultyAttributes] cache of a [DifficultyCalculationParameters].
         *
         * @param parameters The [DifficultyCalculationParameters] to retrieve.
         * @return The [TimedDifficultyAttributes], `null` if not found.
         */
        fun getTimedDifficultyCache(parameters: DifficultyCalculationParameters?) =
            getCache(parameters, timedAttributeCache)

        /**
         * Whether this [BeatmapDifficultyCacheManager] does not hold any cache.
         */
        val isEmpty: Boolean
            get() = attributeCache.isEmpty() && timedAttributeCache.isEmpty()

        /**
         * Invalidates all expired cache in this manager.
         *
         * @param currentTime The time to invalidate the cache against, in milliseconds.
         */
        fun invalidateExpiredCache(currentTime: Long) {
            invalidateExpiredCache(currentTime, attributeCache)
            invalidateExpiredCache(currentTime, timedAttributeCache)
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
