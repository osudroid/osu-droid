package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.StandardHitWindow
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes
import com.rian.osu.difficulty.attributes.StandardPerformanceAttributes
import com.rian.osu.difficulty.skills.HarmonicSkill
import com.rian.osu.difficulty.skills.StandardFlashlight
import com.rian.osu.difficulty.skills.VariableLengthStrainSkill
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.math.ErrorFunction
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModNoFail
import com.rian.osu.mods.ModRelax
import com.rian.osu.mods.ModScoreV2
import com.rian.osu.mods.ModTraceable
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A performance calculator for calculating osu!standard performance points.
 */
class StandardPerformanceCalculator(
    /**
     * The [StandardDifficultyAttributes] being calculated.
     */
    difficultyAttributes: StandardDifficultyAttributes
) : PerformanceCalculator<
    StandardDifficultyAttributes,
    StandardPerformanceAttributes,
    PerformanceCalculationParameters
>(difficultyAttributes) {
    private var greatWindow = 0.0
    private var okWindow = 0.0
    private var mehWindow = 0.0

    private var approachRate = 0.0
    private var overallDifficulty = 0.0

    private var aimEstimatedSliderBreaks = 0.0
    private var effectiveMissCount = 0.0
    private var speedDeviation = 0.0

    override fun createPerformanceAttributes(attributes: StandardPerformanceAttributes?) = (attributes ?: StandardPerformanceAttributes()).also {
        var multiplier = FINAL_MULTIPLIER

        effectiveMissCount = calculateComboBasedEstimatedMissCount()

        val hitWindow = StandardHitWindow(this.attributes.overallDifficulty)

        greatWindow = hitWindow.greatWindow / this.attributes.clockRate
        okWindow = hitWindow.okWindow / this.attributes.clockRate
        mehWindow = hitWindow.mehWindow / this.attributes.clockRate

        approachRate = StandardDifficultyCalculator.calculateRateAdjustedApproachRate(this.attributes.approachRate, this.attributes.clockRate)
        overallDifficulty = StandardDifficultyCalculator.calculateRateAdjustedOverallDifficulty(this.attributes.overallDifficulty, this.attributes.clockRate)

        if (this.attributes.mods.any { m -> m is ModNoFail }) {
            multiplier *= max(0.9, 1 - 0.02 * effectiveMissCount)
        }

        if (this.attributes.mods.any { m -> m is ModRelax }) {
            // Graph: https://www.desmos.com/calculator/bc9eybdthb
            // We use OD13.3 as maximum since it's the value at which great hit window becomes 0.
            val okMultiplier = 0.75 * max(0.0, if (overallDifficulty > 0) 1 - overallDifficulty / 13.33 else 1.0)
            val mehMultiplier = max(0.0, if (overallDifficulty > 0) 1 - (overallDifficulty / 13.33).pow(5) else 1.0)

            // As we're adding 100s and 50s to an approximated number of combo breaks, the result can be higher
            // than total hits in specific scenarios (which breaks some calculations), so we need to clamp it.
            effectiveMissCount =
                min(effectiveMissCount + countOk * okMultiplier + countMeh * mehMultiplier, totalHits.toDouble())
        }

        aimEstimatedSliderBreaks = calculateEstimatedSliderBreaks(this.attributes.aimTopWeightedSliderFactor)
        speedDeviation = calculateSpeedDeviation()

        it.effectiveMissCount = effectiveMissCount

        it.aim = calculateAimValue()
        it.speed = calculateSpeedValue()
        it.accuracy = calculateAccuracyValue()
        it.flashlight = calculateFlashlightValue()
        it.reading = calculateReadingValue()

        val cognitionValue = StandardDifficultyCalculator.sumCognitionDifficulty(it.reading, it.flashlight)

        it.total = DifficultyCalculationUtils.norm(NORM_EXPONENT, it.aim, it.speed, it.accuracy, cognitionValue) * multiplier
    }

    private fun calculateAimValue(): Double {
        if (attributes.mods.any { it is ModAutopilot }) {
            return 0.0
        }

        var aimDifficulty = attributes.aimDifficulty
        val aimDifficultSliderCount = attributes.aimDifficultSliderCount
        val maxCombo = attributes.maxCombo

        if (aimDifficultSliderCount > 0) {
            val estimateImproperlyFollowedDifficultSliders = if (usingClassicSliderCalculation) {
                // When the score is considered classic (regardless if it was made on old client or not),
                // we consider all missing combo to be dropped difficult sliders.
                min(totalImperfectHits, maxCombo - scoreMaxCombo).toDouble().coerceIn(0.0, aimDifficultSliderCount)
            } else {
                // We add tick misses here since they too mean that the player didn't follow the slider
                // properly. However, we aren't adding misses here because missing slider heads has a harsh
                // penalty by itself and doesn't mean that the rest of the slider wasn't followed properly.
                (nonComboBreakingSliderNestedMisses + comboBreakingSliderNestedMisses).toDouble().coerceIn(0.0, aimDifficultSliderCount)
            }

            aimDifficulty *=
                (1 - attributes.aimSliderFactor) *
                (1 - estimateImproperlyFollowedDifficultSliders / aimDifficultSliderCount).pow(3) +
                        attributes.aimSliderFactor
        }

        var aimValue = VariableLengthStrainSkill.difficultyToPerformance(aimDifficulty)

        // Longer maps are worth more
        val lengthBonus = 0.95 + 0.35 * min(1.0, totalHits / 2000.0) +
                if (totalHits > 2000) log10(totalHits / 2000.0) * 0.5 else 0.0

        aimValue *= lengthBonus

        if (effectiveMissCount > 0) {
            val relevantMissCount = min(effectiveMissCount + aimEstimatedSliderBreaks, totalImperfectHits + comboBreakingSliderNestedMisses.toDouble())
            aimValue *= calculateMissPenalty(relevantMissCount, attributes.aimDifficultStrainCount)
        }

        if (attributes.mods.any { it is ModTraceable }) {
            aimValue *= 1 + calculateTraceableBonus(attributes.aimSliderFactor)
        }

        // Scale the aim value with accuracy.
        aimValue *= accuracy

        return aimValue
    }

    private fun calculateSpeedValue(): Double {
        if (attributes.mods.any { it is ModRelax } || speedDeviation == Double.POSITIVE_INFINITY) {
            return 0.0
        }

        var speedValue = HarmonicSkill.difficultyToPerformance(attributes.speedDifficulty)

        if (effectiveMissCount > 0) {
            val speedEstimatedSliderBreaks = calculateEstimatedSliderBreaks(attributes.speedTopWeightedSliderFactor)
            val relevantMissCount = min(effectiveMissCount + speedEstimatedSliderBreaks, totalImperfectHits + comboBreakingSliderNestedMisses.toDouble())

            speedValue *= calculateMissPenalty(relevantMissCount, attributes.speedDifficultStrainCount)
        }

        if (attributes.mods.any { it is ModTraceable }) {
            speedValue *= 1 + calculateTraceableBonus()
        }

        speedValue *= calculateSpeedHighDeviationNerf()

        // An effective hit window is created based on the speed SR. The higher the speed difficulty, the shorter the hit window.
        // For example, a speed SR of 4 leads to an effective hit window of 20ms, which is OD 10.
        val effectiveHitWindow = 20 * (4 / attributes.speedDifficulty).pow(0.35)

        // Find the proportion of 300s on speed notes assuming the hit window was the effective hit window.
        val effectiveAccuracy = ErrorFunction.erfFast(effectiveHitWindow / speedDeviation)

        // Scale speed value by normalized accuracy.
        speedValue *= effectiveAccuracy.pow(2)

        return speedValue
    }

    private fun calculateAccuracyValue() = attributes.run {
        if (mods.any { it is ModRelax }) {
            return@run 0.0
        }

        // This percentage only considers HitCircles of any value - in this part of the calculation we focus on hitting the timing hit window.
        val hitObjectWithAccuracyCount = hitCircleCount + if (mods.any { it is ModScoreV2 }) sliderCount else 0

        val betterAccuracyPercentage =
            if (hitObjectWithAccuracyCount > 0) max(
                0.0,
                ((countGreat - (totalHits - hitObjectWithAccuracyCount)) * 6.0 + countOk * 2 + countMeh) / (hitObjectWithAccuracyCount * 6)
            )
            else 0.0

        // Lots of arbitrary values from testing.
        // Considering to use derivation from perfect accuracy in a probabilistic manner - assume normal distribution
        var accuracyValue = 1.52163.pow(overallDifficulty) * betterAccuracyPercentage.pow(24.0) * 2.83

        // Bonus for many hit circles - it's harder to keep good accuracy up for longer
        accuracyValue *= (hitObjectWithAccuracyCount / 1000.0).pow(if (hitObjectWithAccuracyCount < 1000) 0.3 else 0.1)

        if (mods.any { it is ModTraceable }) {
            // Decrease bonus for AR>10.
            accuracyValue *= 1 + 0.08 * Interpolation.reverseLinear(approachRate, 11.5, 10.0)
        }

        if (mods.any { it is ModFlashlight }) {
            accuracyValue *= 1.02
        }

        accuracyValue
    }

    private fun calculateFlashlightValue() = attributes.run {
        if (mods.none { it is ModFlashlight }) {
            return@run 0.0
        }

        var flashlightValue = StandardFlashlight.difficultyToPerformance(flashlightDifficulty)

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            flashlightValue *= 0.97 * (1 - (effectiveMissCount / totalHits).pow(0.775)).pow(effectiveMissCount.pow(0.875))
        }

        flashlightValue *= comboScalingFactor

        // Scale the flashlight value with accuracy slightly.
        flashlightValue *= 0.5 + accuracy / 2

        flashlightValue
    }

    private fun calculateReadingValue() = attributes.run {
        var readingValue = HarmonicSkill.difficultyToPerformance(readingDifficulty)

        if (effectiveMissCount > 0) {
            readingValue *=
                calculateMissPenalty(effectiveMissCount + aimEstimatedSliderBreaks, readingDifficultNoteCount)
        }

        // Scale the reading value with accuracy _harshly_.
        readingValue *= accuracy.pow(3)

        readingValue
    }

    /**
     * Estimates a player's deviation on speed notes using [calculateDeviation], assuming worst-case.
     *
     * Treats all speed notes as hit circles.
     */
    private fun calculateSpeedDeviation(): Double {
        if (totalSuccessfulHits == 0) {
            return Double.POSITIVE_INFINITY
        }

        // Calculate accuracy assuming the worst case scenario
        val speedNoteCount = attributes.speedNoteCount +
            (totalHits - attributes.speedNoteCount) * 0.1

        // Assume worst case: all mistakes were on speed notes
        val relevantCountMiss = min(countMiss.toDouble(), speedNoteCount)
        val relevantCountMeh = min(countMeh.toDouble(), speedNoteCount - relevantCountMiss)
        val relevantCountOk = min(countOk.toDouble(), speedNoteCount - relevantCountMiss - relevantCountMeh)
        val relevantCountGreat = max(0.0, speedNoteCount - relevantCountMiss - relevantCountMeh - relevantCountOk)

        return calculateDeviation(relevantCountGreat, relevantCountOk, relevantCountMeh)
    }

    /**
     * Estimates the player's tap deviation based on the OD, given number of greats, oks, mehs and misses,
     * assuming the player's mean hit error is 0. The estimation is consistent in that two SS scores on the
     * same map with the same settings will always return the same deviation.
     *
     * Misses are ignored because they are usually due to misaiming.
     *
     * Greats and oks are assumed to follow a normal distribution, whereas mehs are assumed to follow a uniform distribution.
     */
    private fun calculateDeviation(relevantCountGreat: Double, relevantCountOk: Double, relevantCountMeh: Double): Double {
        if (relevantCountGreat + relevantCountOk + relevantCountMeh <= 0) {
            return Double.POSITIVE_INFINITY
        }

        // The sample proportion of successful hits.
        val n = max(1.0, relevantCountGreat + relevantCountOk)

        // 99% critical value for the normal distribution (one-tailed).
        val z = 2.32634787404

        // Proportion of greats hit on circles, ignoring misses and 50s.
        val p = relevantCountGreat / n

        // We can be 99% confident that the population proportion is at least this value.
        val pLowerBound = (n * p + z * z / 2) / (n + z * z) - z / (n + z * z) * sqrt(n * p * (1 - p) + z * z / 4)
        var deviation: Double

        // Tested max precision for the deviation calculation.
        if (pLowerBound > 0.01) {
            // Compute the deviation assuming greats and oks are normally distributed.
            deviation = greatWindow / (sqrt(2.0) * ErrorFunction.erfInvFast(pLowerBound))

            // Subtract the deviation provided by tails that land outside the ok hit window from the deviation computed above.
            // This is equivalent to calculating the deviation of a normal distribution truncated at +-okHitWindow.
            val okHitWindowTailAmount = sqrt(2 / Math.PI) * okWindow *
                exp(-0.5 * (okWindow / deviation).pow(2)) / (deviation * ErrorFunction.erfFast(okWindow / (sqrt(2.0) * deviation)))

            deviation *= sqrt(1 - okHitWindowTailAmount)
        } else {
            // A tested limit value for the case of a score only containing oks.
            deviation = okWindow / sqrt(3.0)
        }

        // Compute and add the variance for mehs, assuming that they are uniformly distributed.
        val mehVariance = (mehWindow.pow(2) + okWindow * mehWindow + okWindow.pow(2)) / 3

        // Find the total deviation.
        deviation = sqrt(
            ((relevantCountGreat + relevantCountOk) * deviation.pow(2) + relevantCountMeh * mehVariance) / (relevantCountGreat + relevantCountOk + relevantCountMeh)
        )

        return deviation
    }

    /**
     * Calculates multiplier for speed to account for improper tapping based on the deviation and speed difficulty.
     *
     * [Graph](https://www.desmos.com/calculator/dmogdhzofn)
     */
    private fun calculateSpeedHighDeviationNerf(): Double {
        if (speedDeviation == Double.POSITIVE_INFINITY) {
            return 0.0
        }

        val speedValue = HarmonicSkill.difficultyToPerformance(attributes.speedDifficulty)

        // Decide a point where the PP value achieved compared to the speed deviation is assumed to be tapped
        // improperly. Any PP above this point is considered "excess" speed difficulty. This is used to cause
        // PP above the cutoff to scale logarithmically towards the original speed value thus nerfing the value.
        val excessSpeedDifficultyCutoff = 100 + 220 * (22 / speedDeviation).pow(6.5)

        if (speedValue <= excessSpeedDifficultyCutoff) {
            return 1.0
        }

        val scale = 50
        val adjustedSpeedValue = scale * (ln((speedValue - excessSpeedDifficultyCutoff) / scale + 1) + excessSpeedDifficultyCutoff / scale)

        // 220 UR and less are considered tapped correctly to ensure that normal scores will be punished as little as possible
        val t = 1 - Interpolation.reverseLinear(speedDeviation, 22.0, 27.0)

        return Interpolation.linear(adjustedSpeedValue, speedValue, t) / speedValue
    }

    // Miss penalty assumes that a player will miss on the hardest parts of a map,
    // so we use the amount of relatively difficult sections to adjust miss penalty
    // to make it more punishing on maps with lower amount of hard sections.
    private fun calculateMissPenalty(missCount: Double, difficultStrainCount: Double) =
        if (missCount == 0.0) 1.0
        else 0.93 / (missCount / (4 * ln(difficultStrainCount)) + 1)

    private fun calculateEstimatedSliderBreaks(topWeightedSliderFactor: Double): Double {
        if (!usingClassicSliderCalculation || countOk == 0) {
            return 0.0
        }

        val missedComboPercent = 1 - scoreMaxCombo.toDouble() / attributes.maxCombo
        var estimatedSliderBreaks = min(countOk.toDouble(), effectiveMissCount * topWeightedSliderFactor)

        // Scores with more Oks are more likely to have slider breaks.
        val okAdjustment = ((countOk - estimatedSliderBreaks) + 0.5) / countOk

        // There is a low probability of extra slider breaks on effective miss counts close to 1, as score based
        // calculations are good at indicating if only a single break occurred.
        estimatedSliderBreaks *= DifficultyCalculationUtils.smoothstep(effectiveMissCount, 1.0, 2.0)

        return estimatedSliderBreaks * okAdjustment * DifficultyCalculationUtils.logistic(missedComboPercent, 0.33, 15.0)
    }

    private fun calculateComboBasedEstimatedMissCount(): Double {
        var missCount = countMiss.toDouble()

        if (attributes.sliderCount <= 0) {
            return missCount
        }

        if (usingClassicSliderCalculation) {
            // If sliders in the beatmap are hard, it's likely for player to drop sliderends.
            // However, if the beatmap has easy sliders, it's more likely for player to sliderbreak.
            val likelyMissedSliderEndPortion = 0.04 + 0.06 * min(1.0, attributes.aimTopWeightedSliderFactor).pow(2)

            // Consider that full combo is maximum combo minus dropped slider tails since they don't contribute to combo but also don't break it.
            // In classic scores, we can't know the amount of dropped sliders so we estimate to 10% of all sliders in the beatmap.
            val fullComboThreshold =
                attributes.maxCombo -
                // 4 was picked because in a lot of short stream beatmaps with small amount of sliders, there
                // are 2-3 sliders on which sliderends are often dropped. This is a kind of optimization to
                // achieve the most accurate result on average.
                min(4 * likelyMissedSliderEndPortion * attributes.sliderCount, attributes.sliderCount.toDouble())

            if (scoreMaxCombo < fullComboThreshold) {
                missCount = fullComboThreshold / max(1, scoreMaxCombo)
            }

            // In classic scores, there can't be more misses than a sum of all non-perfect judgements.
            missCount = min(missCount, totalImperfectHits.toDouble())

            // Every slider has *at least* 2 combo attributed in classic mechanics.
            // If they broke on a slider with a tick, then this still works since they would have lost at least 2 combo (the tick and the end).
            // Using this as a max means a score that loses 1 combo on a map can't possibly have been a slider break.
            // It must have been a slider end.
            val maxPossibleSliderBreaks = min(attributes.sliderCount, (attributes.maxCombo - scoreMaxCombo) / 2)
            val sliderBreaks = missCount - countMiss

            if (sliderBreaks > maxPossibleSliderBreaks) {
                missCount = countMiss.toDouble() + maxPossibleSliderBreaks
            }
        } else {
            val fullComboThreshold = attributes.maxCombo.toDouble() - nonComboBreakingSliderNestedMisses

            if (scoreMaxCombo < fullComboThreshold) {
                missCount = fullComboThreshold / max(1, scoreMaxCombo)
            }

            // Combine regular misses with combo-breaking misses because they break combo as well.
            missCount = min(missCount, comboBreakingSliderNestedMisses + countMiss.toDouble())
        }

        return missCount
    }

    private fun calculateTraceableBonus(sliderFactor: Double = 1.0): Double {
        // We want to reward slider aim less, more so at lower AR.
        val highApproachRateSliderVisibilityFactor = 0.5 + sliderFactor.pow(6) / 2
        val lowApproachRateSliderVisibilityFactor = sliderFactor.pow(6)

        var traceableBonus = 0.0275

        // Start from normal curve, rewarding lower AR up to AR7.
        traceableBonus += 0.025 * (12.0 - max(approachRate, 7.0)) * highApproachRateSliderVisibilityFactor

        // For AR up to 0 - reduce reward for very low ARs when object is visible.
        if (approachRate < 7) {
            traceableBonus += 0.025 * (7 - max(approachRate, 0.0)) * lowApproachRateSliderVisibilityFactor
        }

        // Starting from AR0 - cap values so they won't grow to infinity.
        if (approachRate < 0) {
            traceableBonus += 0.025 * (1 - 1.5.pow(approachRate)) * lowApproachRateSliderVisibilityFactor
        }

        return traceableBonus
    }

    private val comboScalingFactor by lazy {
        if (difficultyAttributes.maxCombo <= 0) 0.0
        else min((scoreMaxCombo.toDouble() / difficultyAttributes.maxCombo).pow(0.8), 1.0)
    }

    companion object {
        const val FINAL_MULTIPLIER = 1.12
        const val NORM_EXPONENT = 1.1
    }
}