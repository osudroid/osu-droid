package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.RimuDifficultyAttributes;
import com.rian.difficultycalculator.attributes.RimuPerformanceAttributes;
import com.rian.difficultycalculator.math.ErrorFunction;
import com.rian.difficultycalculator.math.MathUtils;
import com.rian.difficultycalculator.utils.RimuHitWindowConverter;
import com.rian.difficultycalculator.utils.StandardHitWindowConverter;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public class RimuPerformanceCalculator extends PerformanceCalculator {
    public static final double finalMultiplier = 1.24;

    private double effectiveMissCount;
    private double deviation;
    private double tapDeviation;

    public RimuPerformanceCalculator(RimuDifficultyAttributes attributes) {
        super(attributes);
    }

    @Override
    protected RimuPerformanceAttributes createPerformanceAttributes(PerformanceCalculationParameters parameters) {
        double multiplier = finalMultiplier;

        if (difficultyAttributes.mods.contains(GameMod.MOD_NOFAIL)) {
            multiplier *= Math.max(0.9, 1 - 0.02 * effectiveMissCount);
        }

        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            // Graph: https://www.desmos.com/calculator/bc9eybdthb
            // We use OD13.3 as maximum since it's the value at which great hit window becomes 0.
            double okMultiplier = Math.max(0, difficultyAttributes.overallDifficulty > 0 ? 1 - Math.pow(difficultyAttributes.overallDifficulty / 13.33, 1.8) : 1);
            double mehMultiplier = Math.max(0, difficultyAttributes.overallDifficulty > 0 ? 1 - Math.pow(difficultyAttributes.overallDifficulty / 13.33, 5) : 1);

            // As we're adding 100s and 50s to an approximated number of combo breaks, the result can be higher
            // than total hits in specific scenarios (which breaks some calculations),  so we need to clamp it.
            effectiveMissCount = Math.min(effectiveMissCount + countOk * okMultiplier + countMeh * mehMultiplier, getTotalHits());
        }

        RimuPerformanceAttributes attributes = new RimuPerformanceAttributes();

        attributes.effectiveMissCount = effectiveMissCount;
        attributes.aim = calculateAimValue();
        attributes.tap = calculateTapValue();
        attributes.accuracy = calculateAccuracyValue();
        attributes.flashlight = calculateFlashlightValue();
        attributes.visual = calculateVisualValue();

        attributes.total = Math.pow(
                Math.pow(attributes.aim, 1.1) +
                Math.pow(attributes.tap, 1.1) +
                Math.pow(attributes.accuracy, 1.1) +
                Math.pow(attributes.flashlight, 1.1) +
                Math.pow(attributes.visual, 1.1),
                1 / 1.1
        ) * multiplier;

        return attributes;
    }

    @Override
    protected void processParameters(PerformanceCalculationParameters parameters) {
        super.processParameters(parameters);

        effectiveMissCount = calculateEffectiveMissCount();
        deviation = calculateDeviation();
        tapDeviation = calculateTapDeviation();
    }

    @Override
    protected void resetDefaults() {
        super.resetDefaults();

        effectiveMissCount = 0;
        deviation = 0;
        tapDeviation = 0;
    }

    private double calculateAimValue() {
        double aimValue = Math.pow(5 * Math.max(1, Math.pow(difficultyAttributes.aimDifficulty, 0.8) / 0.0675) - 4, 3) / 100000;

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects.
            // Default a 3% reduction for any # of misses.
            aimValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), effectiveMissCount);
        }

        aimValue *= getComboScalingFactor();

        // We assume 15% of sliders in a map are difficult since there's no way to tell from the performance calculator.
        double estimateDifficultSliders = difficultyAttributes.sliderCount * 0.15;

        if (estimateDifficultSliders > 0)
        {
            double estimateSliderEndsDropped = MathUtils.clamp(Math.min(countOk + countMeh + countMiss, difficultyAttributes.maxCombo - scoreMaxCombo), 0, estimateDifficultSliders);
            double sliderNerfFactor = (1 - difficultyAttributes.sliderFactor) * Math.pow(1 - estimateSliderEndsDropped / estimateDifficultSliders, 3) + difficultyAttributes.sliderFactor;
            aimValue *= sliderNerfFactor;
        }

        // Scale the aim value with deviation.
        aimValue *= 1.05 * Math.pow(ErrorFunction.erf(32.0625 / (Math.sqrt(2) * deviation)), 1.5);

        return aimValue;
    }

    private double calculateTapValue() {
        double tapValue = Math.pow(5 * Math.max(1, ((RimuDifficultyAttributes) difficultyAttributes).tapDifficulty / 0.0675) - 4, 3) / 100000;

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects.
            // Default a 3% reduction for any # of misses.
            tapValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), Math.pow(effectiveMissCount, 0.875));
        }

        tapValue *= getComboScalingFactor();

        // Scale the tap value with tap deviation.
        tapValue *= 1.1 * Math.pow(ErrorFunction.erf(25 / (Math.sqrt(2) * tapDeviation)), 1.25);

        return tapValue;
    }

    private double calculateAccuracyValue() {
        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX) || getTotalSuccessfulHits() == 0) {
            return 0;
        }

        double accuracyValue = 650 * Math.exp(-0.125 * deviation) *
                // The following function is to give higher reward for deviations lower than 25 (250 UR).
                (15 / (deviation + 15) + 0.65);

        // Scale the accuracy value with rhythm complexity.
        accuracyValue *= 1.5 / (1 + Math.exp(-(((RimuDifficultyAttributes) difficultyAttributes).rhythmDifficulty - 1) / 2));

        if (difficultyAttributes.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            accuracyValue *= 1.02;
        }

        return accuracyValue;
    }

    private double calculateFlashlightValue() {
        if (!difficultyAttributes.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            return 0;
        }

        double flashlightValue = Math.pow(difficultyAttributes.flashlightDifficulty, 1.6) * 25;

        flashlightValue *= getComboScalingFactor();

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            flashlightValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), Math.pow(effectiveMissCount, 0.875));
        }

        // Account for shorter maps having a higher ratio of 0 combo/100 combo flashlight radius.
        flashlightValue *= 0.7 +  0.1 * Math.min(1, getTotalHits() / 200) +
                        (getTotalHits() > 200 ? 0.2 * Math.min(1, (getTotalHits() - 200) / 200) : 0);

        // Scale the flashlight value with deviation.
        flashlightValue *= ErrorFunction.erf(50 / (Math.sqrt(2) * deviation));

        return flashlightValue;
    }

    private double calculateVisualValue() {
        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            return 0;
        }

        double visualValue = Math.pow((((RimuDifficultyAttributes) difficultyAttributes)).visualDifficulty, 1.6) * 22.5;

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            visualValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), effectiveMissCount);
        }

        visualValue *= getComboScalingFactor();

        // Scale the visual value with object count to penalize short maps.
        visualValue *= Math.min(1, 1.650668 + (0.4845796 - 1.650668) / (1 + Math.pow(getTotalHits() / 817.9306, 1.147469)));

        // Scale the visual value with deviation.
        visualValue *= 1.065 * Math.pow(ErrorFunction.erf(30 / (Math.sqrt(2) * deviation)), 1.75);

        return visualValue;
    }

    /**
     * Estimates the player's tap deviation based on the OD, number of circles and sliders,
     * and number of 300s, 100s, 50s, and misses, assuming the player's mean hit error is 0.
     *
     * The estimation is consistent in that two SS scores on the same map
     * with the same settings will always return the same deviation.
     *
     * Sliders are treated as circles with a 50 hit window.
     *
     * Misses are ignored because they are usually due to misaiming, and 50s
     * are grouped with 100s since they are usually due to misreading.
     *
     * Inaccuracies are capped to the number of circles in the map.
     */
    private double calculateDeviation() {
        if (getTotalSuccessfulHits() == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double greatWindow = StandardHitWindowConverter.odToHitWindow300(difficultyAttributes.overallDifficulty);

        // Obtain the meh hit window for rimu!.
        double clockRate = ((RimuDifficultyAttributes) difficultyAttributes).clockRate;
        double realGreatWindow = greatWindow * clockRate;
        double realStandardOD = StandardHitWindowConverter.hitWindow300ToOD(realGreatWindow);
        double mehWindow = RimuHitWindowConverter.odToHitWindow50(realStandardOD, difficultyAttributes.mods.contains(GameMod.MOD_PRECISE)) / clockRate;

        int greatCountOnCircles = difficultyAttributes.hitCircleCount - countOk - countMeh - countMiss;

        // The probability that a player hits a circle is unknown, but we can estimate it to be
        // the number of greats on circles divided by the number of circles, and then add one
        // to the number of circles as a bias correction / bayesian prior.
        double greatProbabilityCircle = Math.max(0, greatCountOnCircles / (difficultyAttributes.hitCircleCount + 1));
        double greatProbabilitySlider;

        if (greatCountOnCircles < 0) {
            int nonCircleMisses = -greatCountOnCircles;
            greatProbabilitySlider = Math.max(0, (difficultyAttributes.sliderCount - nonCircleMisses) / (difficultyAttributes.sliderCount + 1));
        } else {
            greatProbabilitySlider = difficultyAttributes.sliderCount / (difficultyAttributes.sliderCount + 1d);
        }

        if (greatProbabilityCircle == 0 && greatProbabilitySlider == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double deviationOnCircles = greatWindow / (Math.sqrt(2) * ErrorFunction.erfInv(greatProbabilityCircle));
        double deviationOnSliders = mehWindow / (Math.sqrt(2) * ErrorFunction.erfInv(greatProbabilitySlider));

        return Math.min(deviationOnCircles, deviationOnSliders);
    }

    /**
     * Estimates the player's tap deviation, but only for notes and inaccuracies that are relevant to tap difficulty.
     *
     * Treats all difficult speed notes as circles, so this method can sometimes return a lower deviation than considering all notes.
     * This is fine though, since this method is only used to scale tap pp.
     */
    private double calculateTapDeviation() {
        if (getTotalSuccessfulHits() == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double greatWindow = StandardHitWindowConverter.hitWindow300ToOD(difficultyAttributes.overallDifficulty);
        double relevantTotalDiff = getTotalHits() - difficultyAttributes.speedNoteCount;
        double relevantCountGreat = Math.max(0, countGreat - relevantTotalDiff);

        if (relevantCountGreat == 0) {
            return Double.POSITIVE_INFINITY;
        }

        double greatProbability = relevantCountGreat / (difficultyAttributes.speedNoteCount + 1);

        return greatWindow / (Math.sqrt(2) * ErrorFunction.erfInv(greatProbability));
    }

    private double calculateEffectiveMissCount() {
        // Guess the number of misses + slider breaks from combo
        double comboBasedMissCount = 0;

        if (difficultyAttributes.sliderCount > 0) {
            double fullComboThreshold = difficultyAttributes.maxCombo - 0.1 * difficultyAttributes.sliderCount;

            if (scoreMaxCombo < fullComboThreshold) {
                // Clamp miss count to maximum amount of possible breaks.
                comboBasedMissCount = Math.min(
                        fullComboThreshold / Math.max(1, scoreMaxCombo),
                        countOk + countMeh + countMiss
                );
            }
        }

        return Math.max(countMiss, comboBasedMissCount);
    }

    private double getComboScalingFactor() {
        return difficultyAttributes.maxCombo <= 0 ? 0 : Math.min(Math.pow(scoreMaxCombo, 0.8) / Math.pow(difficultyAttributes.maxCombo, 0.8), 1);
    }
}
