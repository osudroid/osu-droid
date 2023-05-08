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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

/**
 * A helper class for operations relating to difficulty and performance calculation.
 */
public final class BeatmapDifficultyCalculator {
    private static final DifficultyCalculator difficultyCalculator = new DifficultyCalculator();

    /**
     * Cache of difficulty calculations, mapped by MD5 hash.
     */
    private static final HashMap<String, BeatmapDifficultyCache> difficultyCache = new HashMap<>();

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

        if (stat.isEnableForceAR()) {
            parameters.forcedAR = stat.getForceAR();
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
        var cache = difficultyCache.get(beatmap.getMD5());
        if (cache != null) {
            var attributes = cache.getDifficultyCache(parameters);

            if (attributes != null) {
                return attributes;
            }
        }

        var attributes = difficultyCalculator.calculate(
                constructDifficultyBeatmap(beatmap), parameters);

        addCache(beatmap.getMD5(), parameters, attributes);

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
        var cache = difficultyCache.get(beatmap.getMD5());
        if (cache != null) {
            var attributes = cache.getTimedDifficultyCache(parameters);

            if (attributes != null) {
                return attributes;
            }
        }

        var attributes = difficultyCalculator.calculateTimed(
                constructDifficultyBeatmap(beatmap), parameters);

        addCache(beatmap.getMD5(), parameters, attributes);

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
     * @param md5 The MD5 hash of the beatmap to cache.
     * @param parameters The difficulty calculation parameters to cache.
     * @param attributes The difficulty attributes to cache.
     */
    private static void addCache(String md5, DifficultyCalculationParameters parameters,
                                 DifficultyAttributes attributes) {
        var cache = difficultyCache.get(md5);

        if (cache == null) {
            cache = new BeatmapDifficultyCache();
            difficultyCache.put(md5, cache);
        }

        cache.addCache(parameters, attributes);
    }

    /**
     * Adds a cache to the difficulty cache.
     *
     * @param md5 The MD5 hash of the beatmap to cache.
     * @param parameters The difficulty calculation parameters to cache.
     * @param attributes The timed difficulty attributes to cache.
     */
    private static void addCache(String md5, DifficultyCalculationParameters parameters,
                                 List<TimedDifficultyAttributes> attributes) {
        var cache = difficultyCache.get(md5);

        if (cache == null) {
            cache = new BeatmapDifficultyCache();
            difficultyCache.put(md5, cache);
        }

        cache.addCache(parameters, attributes);
    }

    /**
     * A cache holder for a beatmap.
     */
    private static final class BeatmapDifficultyCache {
        private final HashMap<DifficultyCalculationParameters, DifficultyAttributes>
                attributeCache = new HashMap<>();
        private final HashMap<DifficultyCalculationParameters, List<TimedDifficultyAttributes>>
                timedAttributeCache = new HashMap<>();

        /**
         * Adds a difficulty attributes cache.
         *
         * @param parameters The difficulty parameters of the difficulty attributes.
         * @param attributes The difficulty attributes to cache.
         */
        public void addCache(DifficultyCalculationParameters parameters, DifficultyAttributes attributes) {
            if (parameters == null) {
                parameters = new DifficultyCalculationParameters();
            }

            attributeCache.put(parameters.copy(), attributes);
        }

        /**
         * Adds a difficulty attributes cache.
         *
         * @param parameters The difficulty parameters of the difficulty attributes.
         * @param attributes The timed difficulty attributes to cache.
         */
        public void addCache(DifficultyCalculationParameters parameters, List<TimedDifficultyAttributes> attributes) {
            if (parameters == null) {
                parameters = new DifficultyCalculationParameters();
            }

            timedAttributeCache.put(parameters.copy(), attributes);
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
         * Gets the cache of difficulty attributes of a calculation parameter.
         *
         * @param parameters The difficulty calculation parameter to retrieve.
         * @param cacheMap The map containing the cache to lookup for.
         * @return The difficulty attributes, <code>null</code> if not found.
         * @param <T> The difficulty attributes cache type.
         */
        private <T> T getCache(DifficultyCalculationParameters parameters,
                               HashMap<DifficultyCalculationParameters, T> cacheMap) {
            if (parameters == null) {
                parameters = new DifficultyCalculationParameters();
            }

            for (var cache : cacheMap.entrySet()) {
                if (isParameterEqual(cache.getKey(), parameters)) {
                    return cache.getValue();
                }
            }

            return null;
        }

        /**
         * Determines if two calculation parameters are equal.
         *
         * @param parameter1 The first parameter.
         * @param parameter2 The second parameter.
         * @return Whether both calculation parameters are equal.
         */
        private boolean isParameterEqual(DifficultyCalculationParameters parameter1,
                                         DifficultyCalculationParameters parameter2) {
            if (parameter1.customSpeedMultiplier != parameter2.customSpeedMultiplier) {
                return false;
            }

            if (parameter1.isForceAR() != parameter2.isForceAR()) {
                return false;
            }

            // If both parameters enable force AR, check for equality.
            if (parameter1.isForceAR() && parameter2.isForceAR()
                    && parameter1.forcedAR != parameter2.forcedAR) {
                return false;
            }

            // Check whether mods are equal.
            return parameter1.mods.size() == parameter2.mods.size() &&
                    !EnumSet.copyOf(parameter1.mods).retainAll(parameter2.mods);
        }
    }
}