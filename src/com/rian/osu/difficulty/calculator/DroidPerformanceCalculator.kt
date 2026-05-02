package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.difficulty.attributes.DroidPerformanceAttributes
import com.rian.osu.difficulty.skills.DroidFlashlight
import com.rian.osu.difficulty.skills.HarmonicSkill
import com.rian.osu.difficulty.skills.VariableLengthStrainSkill
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.math.ErrorFunction
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModNoFail
import com.rian.osu.mods.ModPrecise
import com.rian.osu.mods.ModRelax
import com.rian.osu.mods.ModScoreV2
import com.rian.osu.replay.SliderCheesePenalty
import kotlin.math.E
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A performance calculator for calculating osu!droid performance points.
 */
class DroidPerformanceCalculator(
    /**
     * The [DroidDifficultyAttributes] being calculated.
     */
    difficultyAttributes: DroidDifficultyAttributes
) : PerformanceCalculator<
    DroidDifficultyAttributes,
    DroidPerformanceAttributes,
    DroidPerformanceCalculationParameters
>(difficultyAttributes) {
    private var effectiveMissCount = 0.0
    private var sliderCheesePenalty = SliderCheesePenalty()
    private var tapPenalty = 1.0
    private var totalScore = 0

    private var aimEstimatedSliderBreaks = 0.0
    private var deviation = 0.0
    private var tapDeviation = 0.0

    private val isPrecise by lazy {
        difficultyAttributes.mods.any { it is ModPrecise }
    }

    override fun createPerformanceAttributes(attributes: DroidPerformanceAttributes?) = (attributes ?: DroidPerformanceAttributes()).also {
        var multiplier = FINAL_MULTIPLIER

        if (usingClassicSliderCalculation) {
            val remainingScore = this.attributes.maximumScore - totalScore

            // If there is less than one miss, let combo-based miss count decide whether this is full combo.
            val scoreBasedMissCount = max(1.0, (totalScore - remainingScore) / remainingScore.toDouble())

            // Cap result by very harsh version of combo-based miss count.
            effectiveMissCount = min(scoreBasedMissCount, calculateMaximumComboBasedMissCount())
        } else {
            effectiveMissCount = calculateComboBasedEstimatedMissCount()
        }

        if (this.attributes.mods.any { m -> m is ModNoFail }) {
            multiplier *= max(0.9, 1 - 0.02 * effectiveMissCount)
        }

        if (this.attributes.mods.any { m -> m is ModRelax }) {
            // Relax scaling was made for osu!standard overall difficulty, so we need to obtain it.
            val hitWindow = hitWindow
            val greatWindow = hitWindow.greatWindow / this.attributes.clockRate
            val od = (79.5 - greatWindow) / 6

            // Graph: https://www.desmos.com/calculator/bc9eybdthb
            // We use OD13.3 as maximum since it's the value at which great hit window becomes 0.
            val okMultiplier = 0.75 * max(0.0, if (od > 0) 1 - od / 13.33 else 1.0)
            val mehMultiplier = max(0.0, if (od > 0) 1 - (od / 13.33).pow(5) else 1.0)

            // As we're adding 100s and 50s to an approximated number of combo breaks, the result can be higher
            // than total hits in specific scenarios (which breaks some calculations), so we need to clamp it.
            effectiveMissCount =
                min(effectiveMissCount + countOk * okMultiplier + countMeh * mehMultiplier, totalHits.toDouble())
        }

        aimEstimatedSliderBreaks = calculateEstimatedSliderBreaks(this.attributes.aimTopWeightedSliderFactor)
        deviation = calculateAimDeviation()
        tapDeviation = calculateTapDeviation()

        it.deviation = deviation
        it.tapDeviation = tapDeviation
        it.effectiveMissCount = effectiveMissCount

        it.aim = calculateAimValue()
        it.tap = calculateTapValue()
        it.accuracy = calculateAccuracyValue()
        it.flashlight = calculateFlashlightValue()
        it.reading = calculateReadingValue()

        val cognitionValue = DroidDifficultyCalculator.sumCognitionDifficulty(it.reading, it.flashlight)

        it.total = DifficultyCalculationUtils.norm(NORM_EXPONENT, it.aim, it.tap, it.accuracy, cognitionValue) * multiplier
    }

    override fun processParameters(parameters: DroidPerformanceCalculationParameters?) = parameters?.let {
        super.processParameters(it)

        it.sliderCheesePenalty.copyTo(sliderCheesePenalty)

        totalScore = it.totalScore
        tapPenalty = it.tapPenalty
    } ?: resetDefaults()

    override fun resetDefaults() {
        super.resetDefaults()

        effectiveMissCount = 0.0
        sliderCheesePenalty.reset()
        totalScore = 0
        tapPenalty = 1.0
    }

    private fun calculateAimValue(): Double {
        if (attributes.mods.any { it is ModAutopilot }) {
            return 0.0
        }

        var aimDifficulty = attributes.aimDifficulty

        if (attributes.aimDifficultSliderCount > 0) {
            val estimateImproperlyFollowedDifficultSliders = if (usingClassicSliderCalculation) {
                // When the score is considered classic (regardless if it was made on old client or not),
                // we consider all missing combo to be dropped difficult sliders.
                min(totalImperfectHits, attributes.maxCombo - scoreMaxCombo).toDouble().coerceIn(0.0, attributes.aimDifficultSliderCount)
            } else {
                // We add tick misses here since they too mean that the player didn't follow the slider
                // properly. However, we aren't adding misses here because missing slider heads has a harsh
                // penalty by itself and doesn't mean that the rest of the slider wasn't followed properly.
                (nonComboBreakingSliderNestedMisses + comboBreakingSliderNestedMisses).toDouble().coerceIn(0.0, attributes.aimDifficultSliderCount)
            }

            aimDifficulty *= attributes.aimSliderFactor +
                (1 - attributes.aimSliderFactor) * (1 - estimateImproperlyFollowedDifficultSliders / attributes.aimDifficultSliderCount).pow(3)
        }

        var aimValue = VariableLengthStrainSkill.difficultyToPerformance(aimDifficulty)

        if (effectiveMissCount > 0) {
            val relevantMissCount = min(
                effectiveMissCount + aimEstimatedSliderBreaks,
                totalImperfectHits + comboBreakingSliderNestedMisses.toDouble()
            )

            aimValue *= min(
                calculateStrainBasedMissPenalty(relevantMissCount, attributes.aimDifficultStrainCount),
                proportionalMissPenalty
            )
        }

        // Scale the aim value with estimated full combo deviation.
        aimValue *= calculateDeviationBasedLengthScaling()

        aimValue *= sliderCheesePenalty.aim

        // Scale the aim value with deviation.
        aimValue *= 1.025 * ErrorFunction.erfFast(25 / (sqrt(2.0) * deviation)).pow(0.475)

        return aimValue
    }

    private fun calculateTapValue(): Double {
        var tapValue = HarmonicSkill.difficultyToPerformance(attributes.tapDifficulty)

        if (effectiveMissCount > 0) {
            val tapEstimatedSliderBreaks = calculateEstimatedSliderBreaks(attributes.tapTopWeightedSliderFactor)

            val relevantMissCount = min(
                effectiveMissCount + tapEstimatedSliderBreaks,
                totalImperfectHits + comboBreakingSliderNestedMisses.toDouble()
            )

            tapValue *= calculateStrainBasedMissPenalty(relevantMissCount, attributes.tapDifficultStrainCount)
        }

        // Scale the tap value with estimated full combo deviation.
        // Consider notes that are difficult to tap with respect to other notes, but
        // also cap the note count to prevent buffing filler patterns.
        tapValue *= min(1.0, calculateDeviationBasedLengthScaling(min(attributes.speedNoteCount, totalHits / 1.45)))

        // An effective hit window is created based on the tap SR. The higher the tap difficulty, the shorter the hit window.
        // For example, a tap SR of 4 leads to an effective hit window of 25ms, which is OD 10 with Precise mod.
        val effectiveHitWindow = 25 * (4 / attributes.tapDifficulty).pow(1.5)

        // Find the proportion of 300s on speed notes assuming the hit window was the effective hit window.
        val effectiveAccuracy = ErrorFunction.erfFast(effectiveHitWindow / tapDeviation)

        // Scale tap value by normalized accuracy.
        tapValue *= effectiveAccuracy.pow(2)

        tapValue *= calculateTapHighDeviationNerf()

        // Scale the tap value with three-fingered penalty.
        tapValue /= tapPenalty

        return tapValue
    }

    private fun calculateAccuracyValue() = attributes.run {
        if (mods.any { it is ModRelax } || totalSuccessfulHits == 0) {
            return@run 0.0
        }

        var accuracyValue = 650 * exp(-0.1 * deviation)

        val accuracyObjectCount =
            if (mods.any { it is ModScoreV2 }) totalHits - spinnerCount
            else hitCircleCount

        // Bonus for many accuracy objects - it is harder to keep good accuracy up for longer.
        accuracyValue *= (sqrt(ln(1 + (E - 1) * accuracyObjectCount / 1000))).pow(if (accuracyObjectCount < 1000) 0.5 else 0.25)

        // Scale the accuracy value with rhythm complexity.
        accuracyValue *= DifficultyCalculationUtils.logistic(rhythmDifficulty, 1.0, 0.5, 1.8)

        // Penalize accuracy pp after the first miss.
        accuracyValue *= 0.97.pow(max(0.0, effectiveMissCount - 1))

        if (mods.any { it is ModFlashlight }) {
            accuracyValue *= 1.02
        }

        accuracyValue
    }

    private fun calculateFlashlightValue() = attributes.run {
        if (mods.none { it is ModFlashlight }) {
            return@run 0.0
        }

        var flashlightValue = DroidFlashlight.difficultyToPerformance(flashlightDifficulty)

        if (effectiveMissCount > 0) {
            // Penalize misses by assessing # of misses relative to the total # of objects. Default a 3% reduction for any # of misses.
            flashlightValue *= 0.97 * (1 - (effectiveMissCount / totalHits).pow(0.775)).pow(effectiveMissCount.pow(0.875))
        }

        // Scale the flashlight value with deviation.
        flashlightValue *= ErrorFunction.erfFast(50 / (sqrt(2.0) * deviation))

        flashlightValue
    }

    private fun calculateReadingValue() = attributes.run {
        var readingValue = HarmonicSkill.difficultyToPerformance(readingDifficulty)

        if (effectiveMissCount > 0) {
            readingValue *= min(
                calculateStrainBasedMissPenalty(
                    effectiveMissCount + aimEstimatedSliderBreaks,
                    readingDifficultNoteCount
                ),
                proportionalMissPenalty
            )
        }

        // Scale the visual value with estimated full combo deviation.
        // As visual is easily "bypassable" with memorization, punish for memorization.
        readingValue *= min(1.0, calculateDeviationBasedLengthScaling(punishForMemorization = true))

        // Scale the visual value with deviation.
        readingValue *= 1.025 * ErrorFunction.erfFast(25 / (sqrt(2.0) * deviation)).pow(1.25)

        readingValue
    }

    private fun calculateStrainBasedMissPenalty(missCount: Double, difficultStrainCount: Double) =
        if (missCount == 0.0) 1.0
        else 0.93 / (missCount / (4 * ln(difficultStrainCount)) + 1)

    private val proportionalMissPenalty by lazy {
        if (effectiveMissCount == 0.0) {
            return@lazy 1.0
        }

        val relevantMissCount = min(
            effectiveMissCount + aimEstimatedSliderBreaks,
            totalImperfectHits + comboBreakingSliderNestedMisses.toDouble()
        )

        if (relevantMissCount == 0.0) {
            return@lazy 1.0
        }

        val missProportion = (totalHits - relevantMissCount) / (totalHits + 1)
        val noMissProportion = totalHits / (totalHits + 1.0)

        // Aim deviation-based scale.
        ErrorFunction.erfInvFast(missProportion) / ErrorFunction.erfInvFast(noMissProportion) *
            // Cheesing-based scale (i.e. 50% misses is deliberately only hitting each other
            // note, 90% misses is deliberately only hitting 1 note every 10 notes).
            missProportion.pow(8)
    }

    /**
     * Calculates the object-based length scaling based on the deviation of a player for a full
     * combo in this beatmap, taking retries into account.
     *
     * @param objectCount The amount of objects to be considered. Defaults to the amount of objects in this beatmap.
     * @param punishForMemorization Whether to punish the deviation for memorization. Defaults to `false`.
     */
    private fun calculateDeviationBasedLengthScaling(objectCount: Double = totalHits.toDouble(),
                                                     punishForMemorization: Boolean = false): Double {
        if (objectCount == 0.0) {
            return 0.0
        }

        // Assume a sample proportion of hits for a full combo to be `(n - 0.5) / n` due to
        // continuity correction, where `n` is the object count.
        fun calculateProportion(notes: Double) = (notes - 0.5) / notes

        // Keeping `x` notes as the benchmark, assume that a player will retry a beatmap
        // `max(1, x/n)` times relative to an `x`-note beatmap.
        val benchmarkNotes = 700.0

        // Calculate the proportion equivalent to the bottom half of retry count percentile of
        // scores and take it as the player's "real" proportion.
        fun retryProportion(proportion: Double, notes: Double, tries: Double) = proportion +
            sqrt(2 * proportion * (1 - proportion) / notes) *
            ErrorFunction.erfInvFast(1 / tries - 1)

        // Using the proportion, we calculate the deviation based off that proportion and again
        // compared to the hit deviation for proportion `(n - 0.5) / n`.
        var multiplier = max(
            0.0,
            ErrorFunction.erfInvFast(
                retryProportion(calculateProportion(objectCount), objectCount, max(1.0, benchmarkNotes / objectCount))
            ) / ErrorFunction.erfInvFast(calculateProportion(benchmarkNotes))
        )

        // Punish for memorization if needed.
        if (punishForMemorization) {
            multiplier *= min(1.0, sqrt(objectCount / benchmarkNotes))
        }

        return multiplier
    }

    /**
     * Estimates the player's deviation based on the OD, number of circles and sliders,
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
    private fun calculateAimDeviation() =
        calculateDeviation(countGreat.toDouble(), countOk.toDouble(), countMeh.toDouble())

    /**
     * Does the same as [calculateAimDeviation], but only for notes and inaccuracies that are relevant to tap difficulty.
     *
     * Treats all difficult speed notes as circles, so this method can sometimes return a lower deviation than [calculateDeviation].
     * This is fine though, since this method is only used to scale tap pp.
     */
    private fun calculateTapDeviation(): Double {
        if (totalSuccessfulHits == 0) {
            return Double.POSITIVE_INFINITY
        }

        // Calculate accuracy assuming the worst case scenario.
        val speedNoteCount = attributes.speedNoteCount + (totalHits - attributes.speedNoteCount) * 0.1

        // Assume worst case - all non-300s happened in speed notes.
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

        val greatWindow = hitWindow.greatWindow / attributes.clockRate
        val okWindow = hitWindow.okWindow / attributes.clockRate
        val mehWindow = hitWindow.mehWindow / attributes.clockRate

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
     * Calculates a multiplier for tap to account for improper tapping based on the deviation and tap difficulty.
     *
     * [Graph](https://www.desmos.com/calculator/z5l9ebrwpi)
     */
    private fun calculateTapHighDeviationNerf(): Double {
        if (tapDeviation == Double.POSITIVE_INFINITY) {
            return 0.0
        }

        val tapValue = (5 * max(1.0, attributes.tapDifficulty / 0.0675) - 4).pow(3) / 100000

        // Decide a point where the PP value achieved compared to the tap deviation is assumed to be tapped
        // improperly. Any PP above this point is considered "excess" tap difficulty. This is used to cause
        // PP above the cutoff to scale logarithmically towards the original tap value thus nerfing the value.
        val excessTapDifficultyCutoff = 100 + 250 * (25 / tapDeviation).pow(6.5)

        if (tapValue <= excessTapDifficultyCutoff) {
            return 1.0
        }

        val scale = 50
        val adjustedTapValue = scale * (ln((tapValue - excessTapDifficultyCutoff) / scale + 1) + excessTapDifficultyCutoff / scale)

        // 250 UR and less are considered tapped correctly to ensure that normal scores will be punished as little as possible.
        val t = 1 - Interpolation.reverseLinear(tapDeviation, 25.0, 30.0)

        return Interpolation.linear(adjustedTapValue, tapValue, t) / tapValue
    }

    private val hitWindow by lazy {
        if (isPrecise) PreciseDroidHitWindow(difficultyAttributes.overallDifficulty)
        else DroidHitWindow(difficultyAttributes.overallDifficulty)
    }

    private fun calculateEstimatedSliderBreaks(topWeightedSliderFactor: Double): Double {
        val nonMissMistakes = countOk + countMeh

        if (!usingClassicSliderCalculation || nonMissMistakes == 0) {
            return 0.0
        }

        val missedComboPercent = 1 - scoreMaxCombo.toDouble() / attributes.maxCombo
        var estimatedSliderBreaks = min(nonMissMistakes.toDouble(), effectiveMissCount * topWeightedSliderFactor)

        // Scores with more Oks are more likely to have slider breaks.
        val nonMissAdjustment = ((nonMissMistakes - estimatedSliderBreaks) + 0.5) / nonMissMistakes

        // There is a low probability of extra slider breaks on effective miss counts close to 1, as score based
        // calculations are good at indicating if only a single break occurred.
        estimatedSliderBreaks *= DifficultyCalculationUtils.smoothstep(effectiveMissCount, 1.0, 2.0)

        return estimatedSliderBreaks * nonMissAdjustment * DifficultyCalculationUtils.logistic(missedComboPercent, 0.33, 15.0)
    }

    private fun calculateMaximumComboBasedMissCount(): Double {
        var missCount = countMiss.toDouble()

        if (attributes.sliderCount <= 0) {
            return missCount
        }

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
            missCount = (fullComboThreshold / max(1, scoreMaxCombo)).pow(2.5)
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

        return missCount
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

    companion object {
        const val FINAL_MULTIPLIER = 1.24
        const val NORM_EXPONENT = 1.1
    }
}