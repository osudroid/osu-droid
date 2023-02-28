package com.rian.difficultycalculator.calculator;

import com.rian.difficultycalculator.attributes.StandardDifficultyAttributes;
import com.rian.difficultycalculator.attributes.StandardPerformanceAttributes;
import com.rian.difficultycalculator.math.MathUtils;

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

/**
 * A performance calculator for calculating osu!standard performance points.
 */
public class StandardPerformanceCalculator extends PerformanceCalculator {
    public static final double finalMultiplier = 1.14;
    private double effectiveMissCount;

    public StandardPerformanceCalculator(StandardDifficultyAttributes difficultyAttributes) {
        super(difficultyAttributes);
    }

    @Override
    protected StandardPerformanceAttributes createPerformanceAttributes(PerformanceCalculationParameters parameters) {
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

        StandardPerformanceAttributes attributes = new StandardPerformanceAttributes();

        attributes.effectiveMissCount = effectiveMissCount;
        attributes.aim = calculateAimValue();
        attributes.speed = calculateSpeedValue();
        attributes.accuracy = calculateAccuracyValue();
        attributes.flashlight = calculateFlashlightValue();

        attributes.total = Math.pow(
                Math.pow(attributes.aim, 1.1) +
                Math.pow(attributes.speed, 1.1) +
                Math.pow(attributes.accuracy, 1.1) +
                Math.pow(attributes.flashlight, 1.1),
                1 / 1.1
        ) * multiplier;

        return attributes;
    }

    @Override
    protected void processParameters(PerformanceCalculationParameters parameters) {
        super.processParameters(parameters);

        effectiveMissCount = calculateEffectiveMissCount();
    }

    @Override
    protected void resetDefaults() {
        super.resetDefaults();

        effectiveMissCount = 0;
    }

    private double calculateAimValue() {
        double aimValue = Math.pow(5 * Math.max(1, difficultyAttributes.aimDifficulty / 0.0675) - 4, 3) / 100000;

        // Longer maps are worth more
        double lengthBonus = 0.95 + 0.4 * Math.min(1, getTotalHits() / 2000);
        if (getTotalHits() > 2000) {
            lengthBonus += Math.log10(getTotalHits() / 2000d) * 0.5;
        }

        aimValue *= lengthBonus;

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            aimValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), effectiveMissCount);
        }

        if (!difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            // AR scaling
            double approachRateFactor = 0;
            if (difficultyAttributes.approachRate > 10.33) {
                approachRateFactor += 0.3 * (difficultyAttributes.approachRate - 10.33);
            } else if (difficultyAttributes.approachRate< 8) {
                approachRateFactor += 0.05 * (8 - difficultyAttributes.approachRate);
            }

            // Buff for longer maps with high AR.
            aimValue *= 1 + approachRateFactor * lengthBonus;
        }

        // We want to give more reward for lower AR when it comes to aim and HD. This nerfs high AR and buffs lower AR.
        if (difficultyAttributes.mods.contains(GameMod.MOD_HIDDEN)) {
            aimValue *= 1 + 0.04 * (12 - difficultyAttributes.approachRate);
        }

        // We assume 15% of sliders in a map are difficult since there's no way to tell from the performance calculator.
        double estimateDifficultSliders = difficultyAttributes.sliderCount * 0.15;

        if (estimateDifficultSliders > 0)
        {
            double estimateSliderEndsDropped = MathUtils.clamp(Math.min(countOk + countMeh + countMiss, difficultyAttributes.maxCombo - scoreMaxCombo), 0, estimateDifficultSliders);
            double sliderNerfFactor = (1 - difficultyAttributes.sliderFactor) * Math.pow(1 - estimateSliderEndsDropped / estimateDifficultSliders, 3) + difficultyAttributes.sliderFactor;
            aimValue *= sliderNerfFactor;
        }

        // Scale the aim value with accuracy.
        aimValue *= getAccuracy();

        // It is also important to consider accuracy difficulty when doing that.
        aimValue *= 0.98 * Math.pow(difficultyAttributes.overallDifficulty, 2) / 2500;

        return aimValue;
    }

    private double calculateSpeedValue() {
        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            return 0;
        }

        double speedValue = Math.pow(5 * Math.max(1, ((StandardDifficultyAttributes) difficultyAttributes).speedDifficulty / 0.0675) - 4, 3) / 100000;

        // Longer maps are worth more
        double lengthBonus = 0.95 + 0.4 * Math.min(1, getTotalHits() / 2000);
        if (getTotalHits() > 2000) {
            lengthBonus += Math.log10(getTotalSuccessfulHits() / 2000d) * 0.5;
        }

        speedValue *= lengthBonus;

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            speedValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), Math.pow(effectiveMissCount, 0.875));
        }

        speedValue *= getComboScalingFactor();

        // AR scaling
        if (difficultyAttributes.approachRate > 10.33) {
            // Buff for longer maps with high AR.
            speedValue *= 1 + 0.3 * (difficultyAttributes.approachRate - 10.33) * lengthBonus;
        }

        if (difficultyAttributes.mods.contains(GameMod.MOD_HIDDEN)) {
            speedValue *= 1 + 0.04 * (12 - difficultyAttributes.approachRate);
        }

        // Calculate accuracy assuming the worst case scenario.
        double relevantTotalDiff = getTotalHits() - this.difficultyAttributes.speedNoteCount;
        double relevantCountGreat = Math.max(0, countGreat - relevantTotalDiff);
        double relevantCountOk = Math.max(0, countOk - Math.max(0, relevantTotalDiff - countGreat));
        double relevantCountMeh = Math.max(0, countMeh - Math.max(0, relevantTotalDiff - countGreat - countOk));
        double relevantAccuracy = difficultyAttributes.speedNoteCount == 0 ? 0 : (relevantCountGreat * 6 + relevantCountOk * 2 + relevantCountMeh) / (difficultyAttributes.speedNoteCount * 6);

        // Scale the speed value with accuracy and OD.
        speedValue *= (0.95 + Math.pow(difficultyAttributes.overallDifficulty, 2) / 750) * Math.pow((getAccuracy() + relevantAccuracy) / 2, (14.5 - Math.max(difficultyAttributes.overallDifficulty, 8)) / 2);

        // Scale the speed value with # of 50s to punish double-tapping.
        speedValue *= Math.pow(0.99, Math.max(0, countMeh - getTotalHits() / 500));

        return speedValue;
    }

    private double calculateAccuracyValue() {
        if (difficultyAttributes.mods.contains(GameMod.MOD_RELAX)) {
            return 0;
        }

        // This percentage only considers HitCircles of any value - in this part of the calculation we focus on hitting the timing hit window.
        double betterAccuracyPercentage = 0;
        int circleCount = difficultyAttributes.hitCircleCount;

        if (circleCount > 0) {
            betterAccuracyPercentage = Math.max(0, ((countGreat - (getTotalHits() - circleCount)) * 6 + countOk * 2 + countMeh) / (circleCount * 6));
        }

        // Lots of arbitrary values from testing.
        // Considering to use derivation from perfect accuracy in a probabilistic manner - assume normal distribution
        double accuracyValue = Math.pow(1.52163, difficultyAttributes.overallDifficulty) * Math.pow(betterAccuracyPercentage, 24) * 2.83;

        // Bonus for many hit circles - it's harder to keep good accuracy up for longer
        accuracyValue *= Math.min(1.15, Math.pow(circleCount / 1000d, 0.3));

        if (difficultyAttributes.mods.contains(GameMod.MOD_HIDDEN)) {
            accuracyValue *= 1.08;
        }
        if (difficultyAttributes.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            accuracyValue *= 1.02;
        }

        return accuracyValue;
    }

    private double calculateFlashlightValue() {
        if (!difficultyAttributes.mods.contains(GameMod.MOD_FLASHLIGHT)) {
            return 0;
        }

        double flashlightValue = Math.pow(difficultyAttributes.flashlightDifficulty, 2) * 25;

        flashlightValue *= getComboScalingFactor();

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            flashlightValue *= 0.97 * Math.pow(1 - Math.pow(effectiveMissCount / getTotalHits(), 0.775), Math.pow(effectiveMissCount, 0.875));
        }

        // Account for shorter maps having a higher ratio of 0 combo/100 combo flashlight radius.
        flashlightValue *= 0.7 + 0.1 * Math.min(1, getTotalHits() / 200) +
                        (getTotalHits() > 200 ? 0.2 * Math.min(1, (getTotalHits() - 200) / 200) : 0);

        // Scale the flashlight value with accuracy slightly.
        flashlightValue *= 0.5 + getAccuracy() / 2;

        // It is also important to consider accuracy difficulty when doing that.
        flashlightValue *= 0.98 + Math.pow(difficultyAttributes.overallDifficulty, 2) / 2500;

        return flashlightValue;
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
