package com.rian.osu.difficulty.calculator

import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes
import com.rian.osu.difficulty.attributes.StandardPerformanceAttributes
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModNoFail
import com.rian.osu.mods.ModRelax
import com.rian.osu.mods.ModScoreV2
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
        var aimValue = (5 * max(1.0, difficultyAttributes.aimDifficulty / 0.0675) - 4).pow(3.0) / 100000

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

            // We assume 15% of sliders in a map are difficult since there's no way to tell from the performance calculator.
            val estimateDifficultSliders = sliderCount * 0.15
            if (estimateDifficultSliders > 0) {
                val estimateSliderEndsDropped =
                    min(
                        countOk + countMeh + countMiss,
                        maxCombo - scoreMaxCombo
                    ).toDouble().coerceIn(0.0, estimateDifficultSliders)

                val sliderNerfFactor =
                    (1 - aimSliderFactor) *
                            (1 - estimateSliderEndsDropped / estimateDifficultSliders).pow(3.0) + aimSliderFactor

                aimValue *= sliderNerfFactor
            }
        }

        // Scale the aim value with accuracy.
        aimValue *= accuracy

        // It is also important to consider accuracy difficulty when doing that.
        aimValue *= 0.98 + difficultyAttributes.overallDifficulty.pow(2.0) / 2500
        return aimValue
    }

    private fun calculateSpeedValue(): Double {
        if (difficultyAttributes.mods.any { it is ModRelax }) {
            return 0.0
        }

        var speedValue = (5 * max(1.0, difficultyAttributes.speedDifficulty / 0.0675) - 4).pow(3.0) / 100000

        // Longer maps are worth more
        val lengthBonus = 0.95 + 0.4 * min(1.0, totalHits / 2000.0) +
                if (totalHits > 2000) log10(totalHits / 2000.0) * 0.5 else 0.0

        speedValue *= lengthBonus
        speedValue *= calculateMissPenalty(difficultyAttributes.speedDifficultStrainCount)

        difficultyAttributes.apply {
            // AR scaling
            if (approachRate > 10.33) {
                // Buff for longer maps with high AR.
                speedValue *= 1 + 0.3 * (approachRate - 10.33) * lengthBonus
            }
            if (mods.any { it is ModHidden }) {
                speedValue *= 1 + 0.04 * (12 - approachRate)
            }

            // Calculate accuracy assuming the worst case scenario.
            val relevantTotalDiff = totalHits - speedNoteCount
            val relevantCountGreat = max(0.0, countGreat - relevantTotalDiff)
            val relevantCountOk = max(0.0, countOk - max(0.0, relevantTotalDiff - countGreat))
            val relevantCountMeh = max(0.0, countMeh - max(0.0, relevantTotalDiff - countGreat - countOk))
            val relevantAccuracy =
                if (speedNoteCount == 0.0) 0.0
                else (relevantCountGreat * 6 + relevantCountOk * 2 + relevantCountMeh) / (speedNoteCount * 6)

            // Scale the speed value with accuracy and OD.
            speedValue *= (0.95 + overallDifficulty.pow(2.0) / 750) *
                        ((accuracy + relevantAccuracy) / 2).pow((14.5 - overallDifficulty) / 2)
        }

        // Scale the speed value with # of 50s to punish double-tapping.
        speedValue *= 0.99.pow(max(0.0, countMeh - totalHits / 500.0))

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

        return difficultyAttributes.run {
            // Lots of arbitrary values from testing.
            // Considering to use derivation from perfect accuracy in a probabilistic manner - assume normal distribution
            var accuracyValue =
                1.52163.pow(overallDifficulty) * betterAccuracyPercentage.pow(24.0) * 2.83

            // Bonus for many hit circles - it's harder to keep good accuracy up for longer
            accuracyValue *= min(1.15, (hitObjectWithAccuracyCount / 1000.0).pow(0.3))

            if (mods.any { it is ModHidden }) {
                accuracyValue *= 1.08
            }
            if (mods.any { it is ModFlashlight }) {
                accuracyValue *= 1.02
            }

            accuracyValue
        }
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