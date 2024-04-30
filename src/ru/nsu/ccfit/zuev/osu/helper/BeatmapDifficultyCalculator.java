package ru.nsu.ccfit.zuev.osu.helper;

import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.attributes.PerformanceAttributes;
import com.rian.difficultycalculator.attributes.TimedDifficultyAttributes;
import com.rian.difficultycalculator.beatmap.BeatmapDifficultyManager;
import com.rian.difficultycalculator.beatmap.DifficultyBeatmap;
import com.rian.difficultycalculator.calculator.DifficultyCalculationParameters;
import com.rian.difficultycalculator.calculator.DifficultyCalculator;
import com.rian.difficultycalculator.calculator.PerformanceCalculationParameters;
import com.rian.difficultycalculator.calculator.PerformanceCalculator;

import java.util.HashMap;
import java.util.List;

import com.rian.difficultycalculator.utils.LRUCache;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

/**
 * A helper class for operations relating to difficulty and performance calculation.
 */
public final class BeatmapDifficultyCalculator {
    private static final DifficultyCalculator difficultyCalculator = new DifficultyCalculator();

    /**
     * Cache of difficulty calculations, mapped by MD5 hash of a beatmap.
     */
    private static final LRUCache<String, BeatmapDifficultyCacheManager> difficultyCacheManager = new LRUCache<>(10);

    /**
     * Constructs a <code>DifficultyCalculationParameters</code> from a <code>StatisticV2</code>.
     *
     * @param stat The <code>StatisticV2</code> to construct the <code>DifficultyCalculationParameters</code> from.
     * @return The <code>DifficultyCalculationParameters</code> representing the <code>StatisticV2</code>,
     * <code>null</code> if the <code>StatisticV2</code> instance is <code>null</code>.
     */
    public static DifficultyCalculationParameters constructDifficultyParameters(final StatisticV2 stat) {
        if (stat == null) {
            return null;
        }

        DifficultyCalculationParameters parameters = new DifficultyCalculationParameters();

        parameters.mods = stat.getMod().clone();
        parameters.customSpeedMultiplier = stat.getChangeSpeed();

        if (stat.isCustomCS()) {
            parameters.customCS = stat.getCustomCS();
        }
        if (stat.isCustomAR()) {
            parameters.customAR = stat.getCustomAR();
        }
        if (stat.isCustomOD()) {
            parameters.customOD = stat.getCustomOD();
        }

        return parameters;
    }

    /**
     * Constructs a <code>PerformanceCalculationParameters</code> from a <code>StatisticV2</code>.
     *
     * @param stat The <code>StatisticV2</code> to construct the <code>PerformanceCalculationParameters</code> from.
     * @return The <code>PerformanceCalculationParameters</code> representing the <code>StatisticV2</code>,
     * <code>null</code> if the <code>StatisticV2</code> instance is <code>null</code>.
     */
    public static PerformanceCalculationParameters constructPerformanceParameters(final StatisticV2 stat) {
        if (stat == null) {
            return null;
        }

        PerformanceCalculationParameters parameters = new PerformanceCalculationParameters();

        parameters.maxCombo = stat.getMaxCombo();
        parameters.countGreat = stat.getHit300();
        parameters.countOk = stat.getHit100();
        parameters.countMeh = stat.getHit50();
        parameters.countMiss = stat.getMisses();

        return parameters;
    }

    /**
     * Calculates the difficulty of a <code>BeatmapData</code>.
     *
     * @param beatmap The <code>BeatmapData</code> to calculate.
     * @return A structure describing the difficulty of the <code>BeatmapData</code>.
     */
    public static DifficultyAttributes calculateDifficulty(final BeatmapData beatmap) {
        return calculateDifficulty(beatmap, (DifficultyCalculationParameters) null);
    }

    /**
     * Calculates the difficulty of a <code>BeatmapData</code> given a <code>StatisticV2</code>.
     *
     * @param beatmap The <code>BeatmapData</code> to calculate.
     * @param stat The <code>StatisticV2</code> to calculate.
     * @return A structure describing the difficulty of the <code>BeatmapData</code>
     * relating to the <code>StatisticV2</code>.
     */
    public static DifficultyAttributes calculateDifficulty(
            final BeatmapData beatmap, final StatisticV2 stat) {
        return calculateDifficulty(beatmap, constructDifficultyParameters(stat));
    }

    /**
     * Calculates the difficulty of a <code>BeatmapData</code>.
     *
     * @param beatmap The <code>BeatmapData</code> to calculate.
     * @param parameters The parameters of the calculation. Can be <code>null</code>.
     * @return A structure describing the difficulty of the <code>BeatmapData</code>
     * relating to the calculation parameters.
     */
    public static DifficultyAttributes calculateDifficulty(
            final BeatmapData beatmap, final DifficultyCalculationParameters parameters) {
        var cacheManager = difficultyCacheManager.get(beatmap.getMD5());

        if (cacheManager != null) {
            var attributes = cacheManager.getDifficultyCache(parameters);

            if (attributes != null) {
                return attributes;
            }
        }

        var attributes = difficultyCalculator.calculate(
                constructDifficultyBeatmap(beatmap), parameters);

        addCache(beatmap, parameters, attributes);

        return attributes;
    }

    /**
     * Calculates the difficulty of a <code>BeatmapData</code>, returning a set of
     * <code>TimedDifficultyAttributes</code> representing the difficulty of the beatmap
     * at any relevant time.
     *
     * @param beatmap The <code>BeatmapData</code> to calculate.
     * @return A set of <code>TimedDifficultyAttributes</code> describing the difficulty of
     * the <code>BeatmapData</code> at any relevant time.
     */
    public static List<TimedDifficultyAttributes> calculateTimedDifficulty(
            final BeatmapData beatmap) {
        return calculateTimedDifficulty(beatmap, (DifficultyCalculationParameters) null);
    }

    /**
     * Calculates the difficulty of a <code>BeatmapData</code>, returning a set of
     * <code>TimedDifficultyAttributes</code> representing the difficulty of the beatmap
     * at any relevant time.
     *
     * @param beatmap The <code>BeatmapData</code> to calculate.
     * @param stat The <code>StatisticV2</code> to calculate.
     * @return A set of <code>TimedDifficultyAttributes</code> describing the difficulty of
     * the <code>BeatmapData</code> at any relevant time relating to the <code>StatisticV2</code>.
     */
    public static List<TimedDifficultyAttributes> calculateTimedDifficulty(
            final BeatmapData beatmap, final StatisticV2 stat) {
        return calculateTimedDifficulty(beatmap, constructDifficultyParameters(stat));
    }

    /**
     * Calculates the difficulty of a <code>BeatmapData</code> given a <code>StatisticV2</code>,
     * returning a set of <code>TimedDifficultyAttributes</code> representing the difficulty of the
     * beatmap at any relevant time.
     *
     * @param beatmap The <code>BeatmapData</code> to calculate.
     * @param parameters The parameters of the calculation. Can be <code>null</code>.
     * @return A set of <code>TimedDifficultyAttributes</code> describing the difficulty of
     * the <code>BeatmapData</code> at any relevant time relating to the calculation parameters.
     */
    public static List<TimedDifficultyAttributes> calculateTimedDifficulty(
            final BeatmapData beatmap, final DifficultyCalculationParameters parameters) {
        var cacheManager = difficultyCacheManager.get(beatmap.getMD5());

        if (cacheManager != null) {
            var attributes = cacheManager.getTimedDifficultyCache(parameters);

            if (attributes != null) {
                return attributes;
            }
        }

        var attributes = difficultyCalculator.calculateTimed(
                constructDifficultyBeatmap(beatmap), parameters);

        addCache(beatmap, parameters, attributes);

        return attributes;
    }

    /**
     * Calculates the performance of a <code>DifficultyAttributes</code>.
     *
     * @param attributes The <code>DifficultyAttributes</code> to calculate.
     * @return A structure describing the performance of the <code>DifficultyAttributes</code>.
     */
    public static PerformanceAttributes calculatePerformance(
            final DifficultyAttributes attributes) {
        return calculatePerformance(attributes, (PerformanceCalculationParameters) null);
    }

    /**
     * Calculates the performance of a <code>DifficultyAttributes</code> given a <code>StatisticV2</code>.
     *
     * @param attributes The <code>DifficultyAttributes</code> to calculate.
     * @param stat The <code>StatisticV2</code> to calculate.
     * @return A structure describing the performance of the <code>DifficultyAttributes</code>
     * relating to the <code>StatisticV2</code>.
     */
    public static PerformanceAttributes calculatePerformance(
            final DifficultyAttributes attributes, final StatisticV2 stat) {
        return calculatePerformance(attributes, constructPerformanceParameters(stat));
    }

    /**
     * Calculates the performance of a <code>DifficultyAttributes</code>.
     *
     * @param attributes The <code>DifficultyAttributes</code> to calculate.
     * @param parameters The parameters of the calculation. Can be <code>null</code>.
     * @return A structure describing the performance of the <code>DifficultyAttributes</code>
     * relating to the calculation parameters.
     */
    public static PerformanceAttributes calculatePerformance(
            final DifficultyAttributes attributes, final PerformanceCalculationParameters parameters) {
        return new PerformanceCalculator(attributes).calculate(parameters);
    }

    /**
     * Invalidates expired cache.
     */
    public static void invalidateExpiredCache() {
        long currentTime = System.currentTimeMillis();

        for (var iterator = difficultyCacheManager.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next().getValue();

            entry.invalidateExpiredCache(currentTime);

            if (entry.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * Constructs a <code>DifficultyBeatmap</code> from a <code>BeatmapData</code>.
     *
     * @param data The <code>BeatmapData</code> to construct the <code>DifficultyBeatmap</code> from.
     * @return The constructed <code>DifficultyBeatmap</code>.
     */
    private static DifficultyBeatmap constructDifficultyBeatmap(final BeatmapData data) {
        BeatmapDifficultyManager difficultyManager = new BeatmapDifficultyManager();
        difficultyManager.setCS(data.difficulty.cs);
        difficultyManager.setAR(data.difficulty.ar);
        difficultyManager.setOD(data.difficulty.od);
        difficultyManager.setHP(data.difficulty.hp);
        difficultyManager.setSliderMultiplier(data.difficulty.sliderMultiplier);
        difficultyManager.setSliderTickRate(data.difficulty.sliderTickRate);

        DifficultyBeatmap beatmap = new DifficultyBeatmap(difficultyManager, data.hitObjects);
        beatmap.setFormatVersion(data.getFormatVersion());
        beatmap.setStackLeniency(data.general.stackLeniency);

        return beatmap;
    }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The beatmap to cache.
     * @param parameters The difficulty calculation parameters to cache.
     * @param attributes The difficulty attributes to cache.
     */
    private static void addCache(BeatmapData beatmap, DifficultyCalculationParameters parameters,
                                 DifficultyAttributes attributes) {
        var md5 = beatmap.getMD5();
        var cacheManager = difficultyCacheManager.get(md5);

        if (cacheManager == null) {
            cacheManager = new BeatmapDifficultyCacheManager();
            difficultyCacheManager.put(md5, cacheManager);
        }

        cacheManager.addCache(parameters, attributes, 60 * 1000);
    }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param beatmap The beatmap to cache.
     * @param parameters The difficulty calculation parameters to cache.
     * @param attributes The timed difficulty attributes to cache.
     */
    private static void addCache(BeatmapData beatmap, DifficultyCalculationParameters parameters,
                                 List<TimedDifficultyAttributes> attributes) {
        var md5 = beatmap.getMD5();
        var cacheManager = difficultyCacheManager.get(md5);

        if (cacheManager == null) {
            cacheManager = new BeatmapDifficultyCacheManager();
            difficultyCacheManager.put(md5, cacheManager);
        }

        // Allow a maximum of 5 minutes of living cache.
        cacheManager.addCache(parameters, attributes, Math.min(beatmap.getDuration(), 5 * 60 * 1000));
    }

    /**
     * A cache holder for a beatmap.
     */
    private static final class BeatmapDifficultyCacheManager {
        private final LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<DifficultyAttributes>>
                attributeCache = new LRUCache<>(5);
        private final LRUCache<DifficultyCalculationParameters, BeatmapDifficultyCache<List<TimedDifficultyAttributes>>>
                timedAttributeCache = new LRUCache<>(3);

        /**
         * Adds a difficulty attributes cache.
         *
         * @param parameters The difficulty parameters of the difficulty attributes.
         * @param attributes The difficulty attributes to cache.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         */
        public void addCache(DifficultyCalculationParameters parameters, DifficultyAttributes attributes,
                             long timeToLive) {
            addCache(parameters, attributes, attributeCache, timeToLive);
        }

        /**
         * Adds a difficulty attributes cache.
         *
         * @param parameters The difficulty parameters of the difficulty attributes.
         * @param attributes The timed difficulty attributes to cache.
         * @param timeToLive The duration at which this cache is allowed to live, in milliseconds.
         */
        public void addCache(DifficultyCalculationParameters parameters, List<TimedDifficultyAttributes> attributes,
                             long timeToLive) {
            addCache(parameters, attributes, timedAttributeCache, timeToLive);
        }

        /**
         * Retrieves the difficulty attributes cache of a calculation parameter.
         *
         * @param parameters The difficulty calculation parameter to retrieve.
         * @return The difficulty attributes, <code>null</code> if not found.
         */
        public DifficultyAttributes getDifficultyCache(DifficultyCalculationParameters parameters) {
            return getCache(parameters, attributeCache);
        }

        /**
         * Retrieves the timed difficulty attributes cache of a calculation parameter.
         *
         * @param parameters The difficulty calculation parameter to retrieve.
         * @return The timed difficulty attributes, <code>null</code> if not found.
         */
        public List<TimedDifficultyAttributes> getTimedDifficultyCache(DifficultyCalculationParameters parameters) {
            return getCache(parameters, timedAttributeCache);
        }

        /**
         * Whether this cache manager does not hold any cache.
         */
        public boolean isEmpty() {
            return attributeCache.isEmpty() && timedAttributeCache.isEmpty();
        }

        /**
         * Invalidates all expired cache in this manager.
         *
         * @param currentTime The time to invalidate the cache against, in milliseconds.
         */
        public void invalidateExpiredCache(long currentTime) {
            invalidateExpiredCache(currentTime, attributeCache);
            invalidateExpiredCache(currentTime, timedAttributeCache);
        }

        /**
         * Invalidates all expired cache of a cache map in this manager.
         *
         * @param currentTime The time to invalidate the cache against, in milliseconds.
         * @param cacheMap The map.
         */
        private <T> void invalidateExpiredCache(long currentTime,
                                                HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>> cacheMap) {
            for (var iterator = cacheMap.entrySet().iterator(); iterator.hasNext();) {
                var entry = iterator.next().getValue();

                if (entry.isExpired(currentTime)) {
                    iterator.remove();
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
         */
        private <T> void addCache(DifficultyCalculationParameters parameters, T cache,
                                  HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>> cacheMap,
                                  long timeToLive) {
            if (parameters != null) {
                // Copy the parameter for caching.
                parameters = parameters.copy();
                parameters.mods.retainAll(difficultyCalculator.difficultyAdjustmentMods);
            } else {
                parameters = new DifficultyCalculationParameters();
            }

            cacheMap.put(parameters, new BeatmapDifficultyCache<>(cache, timeToLive));
        }

        /**
         * Gets the cache of difficulty attributes of a calculation parameter.
         *
         * @param parameters The difficulty calculation parameter to retrieve.
         * @param cacheMap The map containing the cache to lookup for.
         * @return The difficulty attributes, <code>null</code> if not found.
         * @param <T> The difficulty attributes cache type.
         */
        private <T> T getCache(DifficultyCalculationParameters parameters,
                               HashMap<DifficultyCalculationParameters, BeatmapDifficultyCache<T>> cacheMap) {
            if (parameters != null) {
                // Copy the parameter for caching.
                parameters = parameters.copy();
                parameters.mods.retainAll(difficultyCalculator.difficultyAdjustmentMods);
            } else {
                parameters = new DifficultyCalculationParameters();
            }

            for (var cache : cacheMap.entrySet()) {
                if (cache.getKey().equals(parameters)) {
                    return cache.getValue().cache;
                }
            }

            return null;
        }
    }

    /**
     * Represents a beatmap difficulty cache.
     */
    private static final class BeatmapDifficultyCache<T> {
        /**
         * The time at which this cache was generated, in milliseconds.
         */
        public final long generatedTime = System.currentTimeMillis();

        /**
         * The duration at which this cache is allowed to live, in milliseconds.
         */
        public final long timeToLive;

        /**
         * The cached data.
         */
        public final T cache;

        public BeatmapDifficultyCache(T cache, long timeToLive) {
            this.cache = cache;
            this.timeToLive = timeToLive;
        }

        /**
         * Determines whether this cache has expired.
         *
         * @param time The time to test against, in milliseconds.
         * @return Whether the cache has expired.
         */
        public boolean isExpired(long time) {
            return generatedTime + timeToLive < time;
        }
    }
}