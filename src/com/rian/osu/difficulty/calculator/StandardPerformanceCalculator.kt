package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.StandardHitWindow
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes
import com.rian.osu.difficulty.attributes.StandardPerformanceAttributes
import com.rian.osu.math.ErrorFunction
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModNoFail
import com.rian.osu.mods.ModRelax
import com.rian.osu.mods.ModScoreV2
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
    private var speedDeviation = 0.0

    override fun createPerformanceAttributes() = StandardPerformanceAttributes().also {
        var multiplier = FINAL_MULTIPLIER

        difficultyAttributes.run {
            if (mods.any { m -> m is ModNoFail }) {
                multiplier *= max(0.9, 1 - 0.02 * effectiveMissCount)
            }

            if (mods.any { m -> m is ModRelax }) {
                // Graph: https://www.desmos.com/calculator/bc9eybdthb
                // We use OD13.3 as maximum since it's the value at which great hit window becomes 0.
                val okMultiplier = max(
                    0.0,
                    if (overallDifficulty > 0) 1 - (overallDifficulty / 13.33).pow(1.8)
                    else 1.0
                )
                val mehMultiplier = max(
                    0.0,
                    if (overallDifficulty > 0) 1 - (overallDifficulty / 13.33).pow(5.0)
                    else 1.0
                )

                // As we're adding 100s and 50s to an approximated number of combo breaks, the result can be higher
                // than total hits in specific scenarios (which breaks some calculations),  so we need to clamp it.
                effectiveMissCount =
                    min(effectiveMissCount + countOk * okMultiplier + countMeh * mehMultiplier, totalHits.toDouble())
            }
        }

        speedDeviation = calculateSpeedDeviation()

        it.effectiveMissCount = effectiveMissCount
        it.aim = calculateAimValue()
        it.speed = calculateSpeedValue()
        it.accuracy = calculateAccuracyValue()
        it.flashlight = calculateFlashlightValue()
        it.total = (
                it.aim.pow(1.1) +
                        it.speed.pow(1.1) +
                        it.accuracy.pow(1.1) +
                        it.flashlight.pow(1.1)
                ).pow(1 / 1.1) * multiplier
    }

    private fun calculateAimValue(): Double {
        if (difficultyAttributes.mods.any { it is ModAutopilot }) {
            return 0.0
        }

        var aimValue = baseValue(difficultyAttributes.aimDifficulty)

        // Longer maps are worth more
        val lengthBonus = 0.95 + 0.4 * min(1.0, totalHits / 2000.0) +
                if (totalHits > 2000) log10(totalHits / 2000.0) * 0.5 else 0.0

        aimValue *= lengthBonus
        aimValue *= calculateMissPenalty(difficultyAttributes.aimDifficultStrainCount)

        difficultyAttributes.apply {
            if (mods.none { it is ModRelax }) {
                // AR scaling
                var approachRateFactor = 0.0
                if (approachRate > 10.33) {
                    approachRateFactor += 0.3 * (approachRate - 10.33)
                } else if (approachRate < 8) {
                    approachRateFactor += 0.05 * (8 - approachRate)
                }

                // Buff for longer maps with high AR.
                aimValue *= 1 + approachRateFactor * lengthBonus
            }

            // We want to give more reward for lower AR when it comes to aim and HD. This nerfs high AR and buffs lower AR.
            if (mods.any { it is ModHidden }) {
                aimValue *= 1 + 0.04 * (12 - approachRate)
            }

            if (aimDifficultSliderCount > 0) {
                // Consider all missing combo to be dropped difficult sliders.
                val estimateImproperlyFollowedDifficultSliders =
                    min(totalImperfectHits, maxCombo - scoreMaxCombo).toDouble().coerceIn(0.0, aimDifficultSliderCount)

                val sliderNerfFactor =
                    (1 - aimSliderFactor) *
                    (1 - estimateImproperlyFollowedDifficultSliders / aimDifficultSliderCount).pow(3) +
                    aimSliderFactor

                aimValue *= sliderNerfFactor
            }
        }

        // Scale the aim value with accuracy.
        aimValue *= accuracy

        // It is also important to consider accuracy difficulty when doing that.
        aimValue *= 0.98 + max(0.0, difficultyAttributes.overallDifficulty).pow(2.0) / 2500
        return aimValue
    }

    private fun calculateSpeedValue(): Double {
        if (difficultyAttributes.mods.any { it is ModRelax } || speedDeviation == Double.POSITIVE_INFINITY) {
            return 0.0
        }

        var speedValue = baseValue(difficultyAttributes.speedDifficulty)

        // Longer maps are worth more
        val lengthBonus = 0.95 + 0.4 * min(1.0, totalHits / 2000.0) +
                if (totalHits > 2000) log10(totalHits / 2000.0) * 0.5 else 0.0

        speedValue *= lengthBonus
        speedValue *= calculateMissPenalty(difficultyAttributes.speedDifficultStrainCount)

        // AR scaling
        if (difficultyAttributes.approachRate > 10.33 && difficultyAttributes.mods.none { it is ModAutopilot }) {
            // Buff for longer maps with high AR.
            speedValue *= 1 + 0.3 * (difficultyAttributes.approachRate - 10.33) * lengthBonus
        }

        if (difficultyAttributes.mods.any { it is ModHidden }) {
            speedValue *= 1 + 0.04 * (12 - difficultyAttributes.approachRate)
        }

        // Calculate accuracy assuming the worst case scenario.
        val relevantTotalDiff = totalHits - difficultyAttributes.speedNoteCount
        val relevantCountGreat = max(0.0, countGreat - relevantTotalDiff)
        val relevantCountOk = max(0.0, countOk - max(0.0, relevantTotalDiff - countGreat))
        val relevantCountMeh = max(0.0, countMeh - max(0.0, relevantTotalDiff - countGreat - countOk))
        val relevantAccuracy =
            if (difficultyAttributes.speedNoteCount == 0.0) 0.0
            else (relevantCountGreat * 6 + relevantCountOk * 2 + relevantCountMeh) / (difficultyAttributes.speedNoteCount * 6)

        speedValue *= calculateSpeedHighDeviationNerf()

        // Scale the speed value with accuracy and OD.
        speedValue *= (0.95 + max(0.0, difficultyAttributes.overallDifficulty).pow(2.0) / 750) *
            ((accuracy + relevantAccuracy) / 2).pow((14.5 - difficultyAttributes.overallDifficulty) / 2)

        return speedValue
    }

    private fun calculateAccuracyValue(): Double {
        if (difficultyAttributes.mods.any { it is ModRelax }) {
            return 0.0
        }

        // This percentage only considers HitCircles of any value - in this part of the calculation we focus on hitting the timing hit window.
        val hitObjectWithAccuracyCount = difficultyAttributes.hitCircleCount +
            if (difficultyAttributes.mods.any { it is ModScoreV2 }) difficultyAttributes.sliderCount else 0

        val betterAccuracyPercentage =
            if (hitObjectWithAccuracyCount > 0) max(
                0.0,
                ((countGreat - (totalHits - hitObjectWithAccuracyCount)) * 6.0 + countOk * 2 + countMeh) / (hitObjectWithAccuracyCount * 6)
            )
            else 0.0

        // Lots of arbitrary values from testing.
        // Considering to use derivation from perfect accuracy in a probabilistic manner - assume normal distribution
        var accuracyValue = 1.52163.pow(difficultyAttributes.overallDifficulty) * betterAccuracyPercentage.pow(24.0) * 2.83

        // Bonus for many hit circles - it's harder to keep good accuracy up for longer
        accuracyValue *= min(1.15, (hitObjectWithAccuracyCount / 1000.0).pow(0.3))

        if (difficultyAttributes.mods.any { it is ModHidden }) {
            accuracyValue *= 1.08
        }

        if (difficultyAttributes.mods.any { it is ModFlashlight }) {
            accuracyValue *= 1.02
        }

        return accuracyValue
    }

    private fun calculateFlashlightValue(): Double {
        if (difficultyAttributes.mods.none { it is ModFlashlight }) {
            return 0.0
        }

        var flashlightValue = difficultyAttributes.flashlightDifficulty.pow(2.0) * 25

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            flashlightValue *= 0.97 * (1 - (effectiveMissCount / totalHits).pow(0.775)).pow(effectiveMissCount.pow(0.875))
        }

        flashlightValue *= comboScalingFactor

        // Account for shorter maps having a higher ratio of 0 combo/100 combo flashlight radius.
        flashlightValue *=
            0.7 + 0.1 * min(1.0, totalHits / 200.0) +
                    if (totalHits > 200)
                        0.2 * min(1.0, (totalHits - 200.0) / 200)
                    else 0.0

        // Scale the flashlight value with accuracy slightly.
        flashlightValue *= 0.5 + accuracy / 2

        // It is also important to consider accuracy difficulty when doing that.
        flashlightValue *= 0.98 + difficultyAttributes.overallDifficulty.pow(2.0) / 2500

        return flashlightValue
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
        val speedNoteCount = difficultyAttributes.speedNoteCount +
            (totalHits - difficultyAttributes.speedNoteCount) * 0.1

        // Assume worst case: all mistakes were on speed notes
        val relevantCountMiss = min(countMiss.toDouble(), speedNoteCount)
        val relevantCountMeh = min(countMeh.toDouble(), speedNoteCount - relevantCountMiss)
        val relevantCountOk = min(countOk.toDouble(), speedNoteCount - relevantCountMiss - relevantCountMeh)
        val relevantCountGreat = max(0.0, speedNoteCount - relevantCountMiss - relevantCountMeh - relevantCountOk)

        return calculateDeviation(relevantCountGreat, relevantCountOk, relevantCountMeh, relevantCountMiss)
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
    private fun calculateDeviation(relevantCountGreat: Double, relevantCountOk: Double, relevantCountMeh: Double, relevantCountMiss: Double): Double {
        if (relevantCountGreat + relevantCountOk + relevantCountMeh <= 0) {
            return Double.POSITIVE_INFINITY
        }

        val objectCount = relevantCountGreat + relevantCountOk + relevantCountMeh + relevantCountMiss

        // Obtain the great, ok, and meh windows.
        val hitWindow = StandardHitWindow(
            StandardHitWindow.hitWindow300ToOverallDifficulty(
                // Convert current OD to non clock rate-adjusted OD.
                StandardHitWindow(difficultyAttributes.overallDifficulty.toFloat()).greatWindow *
                    difficultyAttributes.clockRate.toFloat()
            )
        )

        val greatWindow = hitWindow.greatWindow
        val okWindow = hitWindow.okWindow
        val mehWindow = hitWindow.mehWindow

        // The probability that a player hits a circle is unknown, but we can estimate it to be
        // the number of greats on circles divided by the number of circles, and then add one
        // to the number of circles as a bias correction.
        val n = max(1.0, objectCount - relevantCountMiss - relevantCountMeh)

        // 99% critical value for the normal distribution (one-tailed).
        val z = 2.32634787404

        // Proportion of greats hit on circles, ignoring misses and 50s.
        val p = relevantCountGreat / n

        // We can be 99% confident that p is at least this value.
        val pLowerBound = (n * p + z * z / 2) / (n + z * z) - z / (n + z * z) * sqrt(n * p * (1 - p) + z * z / 4)

        // Compute the deviation assuming greats and oks are normally distributed, and mehs are uniformly distributed.
        // Begin with greats and oks first. Ignoring mehs, we can be 99% confident that the deviation is not higher than:
        var deviation = greatWindow / (sqrt(2.0) * ErrorFunction.erfInv(pLowerBound))

        val randomValue = sqrt(2 / Math.PI) * okWindow * exp(-0.5 * (okWindow / deviation).pow(2)) /
            (deviation * ErrorFunction.erf(okWindow / (sqrt(2.0) * deviation)))

        deviation *= sqrt(1 - randomValue)

        // Value deviation approach as greatCount approaches 0
        val limitValue = okWindow / sqrt(3.0)

        // If precision is not enough to compute true deviation - use limit value
        if (pLowerBound == 0.0 || randomValue >= 1 || deviation > limitValue) {
            deviation = limitValue
        }

        // Then compute the variance for mehs.
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

        val speedValue = baseValue(difficultyAttributes.speedDifficulty)

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

    private fun baseValue(rating: Double) = (5 * max(1.0, rating / 0.0675) - 4).pow(3) / 100000

    // Miss penalty assumes that a player will miss on the hardest parts of a map,
    // so we use the amount of relatively difficult sections to adjust miss penalty
    // to make it more punishing on maps with lower amount of hard sections.
    private fun calculateMissPenalty(difficultStrainCount: Double) =
        if (effectiveMissCount == 0.0) 1.0
        else 0.96 / (effectiveMissCount / (4 * ln(difficultStrainCount).pow(0.94)) + 1)

    private val comboScalingFactor by lazy {
        if (difficultyAttributes.maxCombo <= 0) 0.0
        else min((scoreMaxCombo.toDouble() / difficultyAttributes.maxCombo).pow(0.8), 1.0)
    }

    companion object {
        const val FINAL_MULTIPLIER = 1.15
    }
}