package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.HitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.StandardHitWindow
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.difficulty.attributes.DroidPerformanceAttributes
import com.rian.osu.math.ErrorFunction
import com.rian.osu.math.Interpolation
import com.rian.osu.mods.*
import com.rian.osu.replay.SliderCheesePenalty
import kotlin.math.*

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
    private var sliderCheesePenalty = SliderCheesePenalty()
    private var tapPenalty = 1.0

    private var deviation = 0.0
    private var tapDeviation = 0.0

    private val isPrecise by lazy {
        difficultyAttributes.mods.any { it is ModPrecise }
    }

    override fun createPerformanceAttributes() = DroidPerformanceAttributes().also {
        var multiplier = FINAL_MULTIPLIER

        difficultyAttributes.run {
            if (mods.any { m -> m is ModNoFail }) {
                multiplier *= max(0.9, 1 - 0.02 * effectiveMissCount)
            }

            if (mods.any { m -> m is ModRelax }) {
                // Graph: https://www.desmos.com/calculator/bc9eybdthb
                // We use OD13.3 as maximum since it's the value at which great hit window becomes 0.
                val okMultiplier = 0.75 * max(
                    0.0,
                    if (overallDifficulty > 0) 1 - overallDifficulty / 13.33 else 1.0
                )

                val mehMultiplier = max(
                    0.0,
                    if (overallDifficulty > 0) 1 - (overallDifficulty / 13.33).pow(5) else 1.0
                )

                // As we're adding 100s and 50s to an approximated number of combo breaks, the result can be higher
                // than total hits in specific scenarios (which breaks some calculations), so we need to clamp it.
                effectiveMissCount =
                    min(effectiveMissCount + countOk * okMultiplier + countMeh * mehMultiplier, totalHits.toDouble())
            }
        }

        deviation = calculateDeviation()
        tapDeviation = calculateTapDeviation()

        it.deviation = deviation
        it.tapDeviation = tapDeviation
        it.effectiveMissCount = effectiveMissCount

        it.aim = calculateAimValue()
        it.tap = calculateTapValue()
        it.accuracy = calculateAccuracyValue()
        it.flashlight = calculateFlashlightValue()
        it.reading = calculateReadingValue()

        it.total = (
            it.aim.pow(1.1) +
            it.tap.pow(1.1) +
            it.accuracy.pow(1.1) +
            it.flashlight.pow(1.1) +
            it.reading.pow(1.1)
        ).pow(1 / 1.1) * multiplier
    }

    override fun processParameters(parameters: DroidPerformanceCalculationParameters?) = parameters?.let {
        super.processParameters(it)

        sliderCheesePenalty = it.sliderCheesePenalty.copy()
        tapPenalty = it.tapPenalty
    } ?: resetDefaults()

    override fun resetDefaults() {
        super.resetDefaults()

        sliderCheesePenalty = SliderCheesePenalty()

        tapPenalty = 1.0
    }

    private fun calculateAimValue() = difficultyAttributes.run {
        var aimValue = (5 * max(1.0, aimDifficulty.pow(0.8) / 0.0675) - 4).pow(3) / 100000

        aimValue *= min(calculateStrainBasedMissPenalty(aimDifficultStrainCount), proportionalMissPenalty)

        // Scale the aim value with estimated full combo deviation.
        aimValue *= calculateDeviationBasedLengthScaling()

        if (aimDifficultSliderCount > 0) {
            val estimateImproperlyFollowedDifficultSliders = if (usingClassicSliderCalculation) {
                // When the score is considered classic (regardless if it was made on old client or not),
                // we consider all missing combo to be dropped difficult sliders.
                min(totalImperfectHits, maxCombo - scoreMaxCombo).toDouble().coerceIn(0.0, aimDifficultSliderCount)
            } else {
                // We add tick misses here since they too mean that the player didn't follow the slider
                // properly. However, we aren't adding misses here because missing slider heads has a harsh
                // penalty by itself and doesn't mean that the rest of the slider wasn't followed properly.
                (nonComboBreakingSliderNestedMisses!! + comboBreakingSliderNestedMisses!!).toDouble().coerceIn(0.0, aimDifficultSliderCount)
            }

            aimValue *=
                (1 - aimSliderFactor) *
                (1 - estimateImproperlyFollowedDifficultSliders / aimDifficultSliderCount).pow(3) +
                aimSliderFactor
        }

        aimValue *= sliderCheesePenalty.aim

        // Scale the aim value with deviation.
        aimValue *= 1.025 * ErrorFunction.erf(25 / (sqrt(2.0) * deviation)).pow(0.475)

        // OD 7 SS stays the same.
        aimValue *= 0.98 + 7.0.pow(2) / 2500

        aimValue
    }

    private fun calculateTapValue() = difficultyAttributes.run {
        var tapValue = (5 * max(1.0, tapDifficulty / 0.0675) - 4).pow(3) / 100000

        tapValue *= calculateStrainBasedMissPenalty(tapDifficultStrainCount)

        // Scale the tap value with estimated full combo deviation.
        // Consider notes that are difficult to tap with respect to other notes, but
        // also cap the note count to prevent buffing filler patterns.
        tapValue *= calculateDeviationBasedLengthScaling(min(speedNoteCount, totalHits / 1.45))

        // Normalize the deviation to 300 BPM.
        val normalizedDeviation = tapDeviation * max(1.0, 50 / averageSpeedDeltaTime)

        // We expect the player to get 7500/x deviation when doubletapping x BPM.
        // Using this expectation, we penalize score with deviation above 25.
        val averageBPM = 60000 / 4 / averageSpeedDeltaTime

        val adjustedDeviation = normalizedDeviation *
            (1 + 1 / (1 + exp(-(normalizedDeviation - 7500 / averageBPM) / (2 * 300 / averageBPM))))

        // Scale the tap value with tap deviation.
        tapValue *= 1.05 * ErrorFunction.erf(20 / (sqrt(2.0) * adjustedDeviation)).pow(0.6)

        // Additional scaling for tap value based on average BPM and how "vibroable" the beatmap is.
        // Higher BPMs require more precise tapping. When the deviation is too high,
        // it can be assumed that the player taps invariant to rhythm.
        // We make the punishment harsher punishment for such scenario.
        tapValue *= vibroFactor.pow(6) +
            (1 - vibroFactor.pow(6)) / (1 + exp((tapDeviation - 7500 / averageBPM) / (2 * 300 / averageBPM)))

        tapValue *= calculateTapHighDeviationNerf()

        // Scale the tap value with three-fingered penalty.
        tapValue /= tapPenalty

        // OD 8 SS stays the same.
        tapValue *= 0.95 + 8.0.pow(2) / 750

        tapValue
    }

    private fun calculateAccuracyValue() = difficultyAttributes.run {
        if (mods.any { it is ModRelax } || totalSuccessfulHits == 0) {
            return@run 0.0
        }

        var accuracyValue = 650 * exp(-0.1 * deviation)

        val accuracyObjectCount =
            if (mods.any { it is ModScoreV2 }) totalHits - spinnerCount
            else hitCircleCount

        // Bonus for many accuracy objects - it is harder to keep good accuracy up for longer.
        accuracyValue *= min(1.15, sqrt(ln(1 + (E - 1) * accuracyObjectCount / 1000)))

        // Scale the accuracy value with rhythm complexity.
        accuracyValue *= 1.5 / (1 + exp(-(rhythmDifficulty - 1) / 2))

        // Penalize accuracy pp after the first miss.
        accuracyValue *= 0.97.pow(max(0.0, effectiveMissCount - 1))

        if (mods.any { it is ModFlashlight }) {
            accuracyValue *= 1.02
        }

        accuracyValue
    }

    private fun calculateFlashlightValue() = difficultyAttributes.run {
        if (mods.none { it is ModFlashlight }) {
            return@run 0.0
        }

        var flashlightValue = flashlightDifficulty.pow(1.6) * 25

        flashlightValue *= min(calculateStrainBasedMissPenalty(flashlightDifficultStrainCount), proportionalMissPenalty)

        // Account for shorter maps having a higher ratio of 0 combo/100 combo flashlight radius.
        flashlightValue *= 0.7 + 0.1 * min(1.0, totalHits / 200.0) +
            if (totalHits > 200) 0.2 * min(1.0, (totalHits - 200) / 200.0) else 0.0

        flashlightValue *= sliderCheesePenalty.flashlight

        // Scale the flashlight value with deviation.
        flashlightValue *= ErrorFunction.erf(50 / (sqrt(2.0) * deviation))

        flashlightValue
    }

    private fun calculateReadingValue() = difficultyAttributes.run {
        var readingValue = (readingDifficulty.pow(2) * 25).pow(0.8)

        readingValue *= min(calculateStrainBasedMissPenalty(readingDifficultNoteCount), proportionalMissPenalty)

        // Scale the visual value with estimated full combo deviation.
        // As visual is easily "bypassable" with memorization, punish for memorization.
        readingValue *= calculateDeviationBasedLengthScaling(punishForMemorization = true)

        // Scale the visual value with deviation.
        readingValue *= 1.05 * ErrorFunction.erf(25 / (sqrt(2.0) * deviation))

        // OD 5 SS stays the same.
        readingValue *= 0.98 + 5.0.pow(2) / 2500

        readingValue
    }

    private fun calculateStrainBasedMissPenalty(difficultStrainCount: Double) =
        if (effectiveMissCount == 0.0) 1.0
        else 0.96 / (effectiveMissCount / (4 * ln(difficultStrainCount).pow(0.94)) + 1)

    private val proportionalMissPenalty by lazy {
        if (effectiveMissCount == 0.0) {
            return@lazy 1.0
        }

        val missProportion = (totalHits - effectiveMissCount) / (totalHits + 1)
        val noMissProportion = totalHits / (totalHits + 1.0)

        // Aim deviation-based scale.
        ErrorFunction.erfInv(missProportion) / ErrorFunction.erfInv(noMissProportion) *
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
            ErrorFunction.erfInv(1 / tries - 1)

        // Using the proportion, we calculate the deviation based off that proportion and again
        // compared to the hit deviation for proportion `(n - 0.5) / n`.
        var multiplier = max(
            0.0,
            ErrorFunction.erfInv(
                retryProportion(calculateProportion(objectCount), objectCount, max(1.0, benchmarkNotes / objectCount))
            ) / ErrorFunction.erfInv(calculateProportion(benchmarkNotes))
        )

        // Punish for memorization if needed.
        if (punishForMemorization) {
            multiplier *= min(1.0, sqrt(objectCount / benchmarkNotes))
        }

        return multiplier
    }

    /**
     * Estimates the player's tap deviation based on the OD, number of circles and sliders,
     * and number of 300s, 100s, 50s, and misses, assuming the player's mean hit error is 0.
     *
     * The estimation is consistent in that two SS scores on the same map
     * with the same settings will always return the same deviation.
     *
     * Under non-ScoreV2 scores, sliders are treated as circles with a 50 hit window.
     *
     * Misses are ignored because they are usually due to mis-aiming, and 50s
     * are grouped with 100s since they are usually due to misreading.
     *
     * Inaccuracies are capped to the number of circles in the map.
     */
    private fun calculateDeviation() = difficultyAttributes.run {
        if (totalSuccessfulHits == 0) {
            return@run Double.POSITIVE_INFINITY
        }

        val hitWindow = getConvertedHitWindow()
        val greatWindow = hitWindow.greatWindow / clockRate
        val okWindow = hitWindow.okWindow / clockRate
        val mehWindow = hitWindow.mehWindow / clockRate

        // For non-ScoreV2 scores, assume 100s, 50s, and misses happen on circles.
        // If there are less non-300s on circles than 300s, compute the deviation on circles.
        val isScoreV2 = mods.any { it is ModScoreV2 }
        val hitObjectWithAccuracyCount = hitCircleCount + if (isScoreV2) sliderCount else 0

        val missCountAccuracyObjects = min(countMiss, hitObjectWithAccuracyCount)
        val mehCountAccuracyObjects = min(countMeh, hitObjectWithAccuracyCount - missCountAccuracyObjects)
        val okCountAccuracyObjects = min(countOk, hitObjectWithAccuracyCount - missCountAccuracyObjects - mehCountAccuracyObjects)
        val greatCountAccuracyObjects = max(0, hitObjectWithAccuracyCount - missCountAccuracyObjects - mehCountAccuracyObjects - okCountAccuracyObjects)

        if (greatCountAccuracyObjects > 0) {
            // The probability that a player hits an accuracy object is unknown, but we can estimate it to be
            // the number of greats on circles divided by the number of circles, and then add one
            // to the number of circles as a bias correction / bayesian prior.
            val greatProbabilityAccuracyObjects =
                greatCountAccuracyObjects / (hitObjectWithAccuracyCount - missCountAccuracyObjects - mehCountAccuracyObjects + 1.0)

            // Compute the deviation assuming 300s and 100s are normally distributed, and 50s are uniformly distributed.
            // Begin with the normal distribution first.
            var deviationOnAccuracyObjects = greatWindow / (sqrt(2.0) * ErrorFunction.erfInv(greatProbabilityAccuracyObjects))

            deviationOnAccuracyObjects *=
                sqrt(1 - sqrt(2 / PI) * okWindow * exp(-0.5 * (okWindow / deviationOnAccuracyObjects).pow(2)) /
                    (deviationOnAccuracyObjects * ErrorFunction.erf(okWindow / (sqrt(2.0) * deviationOnAccuracyObjects))))

            // Then compute the variance for 50s.
            val mehVariance = (mehWindow.pow(2) + mehWindow * okWindow + okWindow.pow(2)) / 3

            // Find the total deviation.
            return@run sqrt(
                ((greatCountAccuracyObjects + okCountAccuracyObjects) * deviationOnAccuracyObjects.pow(2) + mehCountAccuracyObjects * mehVariance) /
                    (greatCountAccuracyObjects + okCountAccuracyObjects + mehCountAccuracyObjects)
            )
        }

        // If there are more non-300s than there are circles, compute the deviation on sliders instead.
        // Here, all that matters is whether the slider was missed, since it is impossible
        // to get a 100 or 50 on a slider by mis-tapping it.

        // For ScoreV2 scores, sliders are already included as accuracy objects, so this part of the computation is invalid.
        if (isScoreV2) {
            return@run Double.POSITIVE_INFINITY
        }

        val missCountSliders = min(sliderCount, countMiss - missCountAccuracyObjects)
        val greatCountSliders = sliderCount - missCountSliders

        // We only get here if nothing was hit. In this case, there is no estimate for deviation.
        // Note that this is never negative, so checking if this is only equal to 0 makes sense.
        if (greatCountSliders == 0) {
            return@run Double.POSITIVE_INFINITY
        }

        val greatProbabilitySlider = greatCountSliders / (sliderCount + 1.0)

        mehWindow / (sqrt(2.0) * ErrorFunction.erfInv(greatProbabilitySlider))
    }

    /**
     * Does the same as [calculateDeviation], but only for notes and inaccuracies that are relevant to tap difficulty.
     *
     * Treats all difficult speed notes as circles, so this method can sometimes return a lower deviation than [calculateDeviation].
     * This is fine though, since this method is only used to scale tap pp.
     */
    private fun calculateTapDeviation() = difficultyAttributes.run {
        if (totalSuccessfulHits == 0) {
            return@run Double.POSITIVE_INFINITY
        }

        val hitWindow = getConvertedHitWindow()
        val greatWindow = hitWindow.greatWindow / clockRate
        val okWindow = hitWindow.okWindow / clockRate
        val mehWindow = hitWindow.mehWindow / clockRate

        // Assume a fixed ratio of non-300s hit in speed notes based on speed note count ratio and OD.
        // Graph: https://www.desmos.com/calculator/eayyireisv
        val speedNoteRatio = speedNoteCount / totalHits
        val nonGreatRatio = 1 - ((exp(sqrt(greatWindow)) + 1).pow(1 - speedNoteRatio) - 1) / exp(sqrt(greatWindow))

        // Assume worst case - all non-300s happened in speed notes.
        val relevantCountMiss = min(countMiss * nonGreatRatio, speedNoteCount)
        val relevantCountMeh = min(countMeh * nonGreatRatio, speedNoteCount - relevantCountMiss)
        val relevantCountOk = min(countOk * nonGreatRatio, speedNoteCount - relevantCountMiss - relevantCountMeh)
        val relevantCountGreat = max(0.0, speedNoteCount - relevantCountMiss - relevantCountMeh - relevantCountOk)

        if (relevantCountGreat + relevantCountOk + relevantCountMeh <= 0) {
            return@run Double.POSITIVE_INFINITY
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
            deviation = greatWindow / (sqrt(2.0) * ErrorFunction.erfInv(pLowerBound))

            // Subtract the deviation provided by tails that land outside the ok hit window from the deviation computed above.
            // This is equivalent to calculating the deviation of a normal distribution truncated at +-okHitWindow.
            val okHitWindowTailAmount = sqrt(2 / Math.PI) * okWindow *
                exp(-0.5 * (okWindow / deviation).pow(2)) / (deviation * ErrorFunction.erf(okWindow / (sqrt(2.0) * deviation)))

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

        return@run deviation
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

        val tapValue = (5 * max(1.0, difficultyAttributes.tapDifficulty / 0.0675) - 4).pow(3) / 100000

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

    private fun getConvertedHitWindow(): HitWindow {
        var od = difficultyAttributes.overallDifficulty.toFloat()
        val hitWindow = StandardHitWindow(od)
        val realGreatWindow = hitWindow.greatWindow * difficultyAttributes.clockRate.toFloat()

        // Obtain the good and meh hit window for osu!droid.
        od =
            if (isPrecise) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(realGreatWindow)
            else DroidHitWindow.hitWindow300ToOverallDifficulty(realGreatWindow)

        return if (isPrecise) PreciseDroidHitWindow(od) else DroidHitWindow(od)
    }

    companion object {
        const val FINAL_MULTIPLIER = 1.24
    }
}