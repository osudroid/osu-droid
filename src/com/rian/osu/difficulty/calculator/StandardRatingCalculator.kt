package com.rian.osu.difficulty.calculator

import com.rian.osu.math.Interpolation
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModRelax
import com.rian.osu.mods.ModTraceable
import com.rian.osu.utils.ModHashMap
import kotlin.math.cbrt
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class StandardRatingCalculator(
    private val mods: ModHashMap,
    private val totalHits: Int,
    private val approachRate: Double,
    private val overallDifficulty: Double,
    private val mechanicalDifficultyRating: Double,
    private val sliderFactor: Double
) {
    fun computeAimRating(aimDifficultyValue: Double): Double {
        if (ModAutopilot::class in mods) {
            return 0.0
        }

        var aimRating = calculateDifficultyRating(aimDifficultyValue)

        if (ModRelax::class in mods) {
            aimRating *= 0.9
        }

        var ratingMultiplier = 1.0
        val approachRateLengthBonus = 0.95 + 0.4 * min(1.0, totalHits / 2000.0) +
            if (totalHits > 2000) log10(totalHits / 2000.0) * 0.5 else 0.0

        val approachRateFactor = when {
            ModRelax::class in mods -> 0.0
            approachRate > 10.33 -> 0.3 * (approachRate - 10.33)
            approachRate < 8 -> 0.05 * (approachRate - 8)
            else -> 0.0
        }

        // Buff for longer beatmaps with high AR.
        ratingMultiplier += approachRateLengthBonus * approachRateFactor

        if (ModHidden::class in mods) {
            val visibilityFactor = calculateAimVisibilityFactor()

            ratingMultiplier += calculateVisibilityBonus(mods.values, approachRate, visibilityFactor, sliderFactor)
        }

        // It is important to consider accuracy difficulty when scaling with accuracy.
        ratingMultiplier *= 0.98 + max(0.0, overallDifficulty).pow(2) / 2500

        return aimRating * cbrt(ratingMultiplier)
    }

    fun computeSpeedRating(speedDifficultyValue: Double): Double {
        if (ModRelax::class in mods) {
            return 0.0
        }

        var speedRating = calculateDifficultyRating(speedDifficultyValue)

        if (ModAutopilot::class in mods) {
            speedRating *= 0.5
        }

        var ratingMultiplier = 1.0
        val approachRateLengthBonus = 0.95 + 0.4 * min(1.0, totalHits / 2000.0) +
                if (totalHits > 2000) log10(totalHits / 2000.0) * 0.5 else 0.0

        val approachRateFactor = when {
            ModAutopilot::class in mods -> 0.0
            approachRate > 10.33 -> 0.3 * (approachRate - 10.33)
            else -> 0.0
        }

        // Buff for longer beatmaps with high AR.
        ratingMultiplier += approachRateLengthBonus * approachRateFactor

        if (ModHidden::class in mods) {
            val visibilityFactor = calculateSpeedVisibilityFactor()

            ratingMultiplier += calculateVisibilityBonus(mods.values, approachRate, visibilityFactor, sliderFactor)
        }

        ratingMultiplier *= 0.95 + max(0.0, overallDifficulty).pow(2) / 750

        return speedRating * cbrt(ratingMultiplier)
    }

    fun computeFlashlightRating(flashlightDifficultyValue: Double): Double {
        if (ModFlashlight::class !in mods) {
            return 0.0
        }

        var flashlightRating = calculateDifficultyRating(flashlightDifficultyValue)

        if (ModRelax::class in mods) {
            flashlightRating += 0.7
        } else if (ModAutopilot::class in mods) {
            flashlightRating *= 0.4
        }

        var ratingMultiplier = 1.0

        // Account for shorter maps having a higher ratio of 0 combo/100 combo flashlight radius.
        ratingMultiplier *= 0.7 + 0.1 * min(1.0, totalHits / 200.0) +
                (if (totalHits > 200) 0.2 * min(1.0, (totalHits - 200) / 200.0) else 0.0)

        // It is important to consider accuracy difficulty when scaling with accuracy.
        ratingMultiplier *= 0.98 + max(0.0, overallDifficulty).pow(2) / 2500

        return flashlightRating * sqrt(ratingMultiplier)
    }

    private fun calculateAimVisibilityFactor(): Double {
        val approachRateFactorEndpoint = 11.5

        val mechanicalDifficultyFactor = Interpolation.reverseLinear(mechanicalDifficultyRating, 5.0, 10.0)
        val approachRateFactorStartingPoint = Interpolation.linear(9.0, 10.33, mechanicalDifficultyFactor)

        return Interpolation.reverseLinear(approachRate, approachRateFactorEndpoint, approachRateFactorStartingPoint)
    }

    private fun calculateSpeedVisibilityFactor(): Double {
        val approachRateFactorEndpoint = 11.5

        val mechanicalDifficultyFactor = Interpolation.reverseLinear(mechanicalDifficultyRating, 5.0, 10.0)
        val approachRateFactorStartingPoint = Interpolation.linear(10.0, 10.33, mechanicalDifficultyFactor)

        return Interpolation.reverseLinear(approachRate, approachRateFactorEndpoint, approachRateFactorStartingPoint)
    }

    companion object {
        private const val DIFFICULTY_MULTIPLIER = 0.0675

        @JvmStatic
        fun calculateDifficultyRating(difficultyValue: Double) = sqrt(difficultyValue) * DIFFICULTY_MULTIPLIER

        /**
         * Calculates a visibility bonus that is applicable to [ModHidden] and [ModTraceable].
         *
         * @param mods The mods applied to the calculation.
         * @param approachRate The approach rate of the beatmap.
         * @param visibilityFactor The visibility factor to apply.
         * @param sliderFactor The slider factor to apply.
         * @returns The visibility bonus multiplier.
         */
        @JvmStatic
        fun calculateVisibilityBonus(
            mods: Iterable<Mod>,
            approachRate: Double,
            visibilityFactor: Double = 1.0,
            sliderFactor: Double = 1.0
        ): Double {
            val isAlwaysPartiallyVisible = (mods.find { it is ModHidden } as ModHidden?)?.onlyFadeApproachCircles ?: mods.any { it is ModTraceable }

            // Start from normal curve, rewarding lower AR up to AR 7.
            // Traceable forcefully requires a lower reading bonus for now as it is post-applied in pp, which make
            // it multiplicative with the regular AR bonuses.
            // This means it has an advantage over Hidden, so we decrease the multiplier to compensate.
            // This should be removed once we are able to apply Traceable bonuses in star rating (requires real-time
            // difficulty calculations being possible).
            var readingBonus = (if (isAlwaysPartiallyVisible) 0.025 else 0.04) * (12 - max(approachRate, 7.0))

            readingBonus *= visibilityFactor

            // We want to reward slideraim on low AR less.
            val sliderVisibilityFactor = sliderFactor.pow(3)

            // For AR up to 0, reduce reward for very low ARs when object is visible.
            if (approachRate < 7) {
                readingBonus +=
                    (if (isAlwaysPartiallyVisible) 0.02 else 0.045) * (7 - max(approachRate, 0.0)) * sliderVisibilityFactor
            }

            // Starting from AR 0, cap values so they won't grow to infinity.
            if (approachRate < 0) {
                readingBonus +=
                    (if (isAlwaysPartiallyVisible) 0.01 else 0.1) * (1 - 1.5.pow(approachRate)) * sliderVisibilityFactor
            }

            return readingBonus
        }
    }
}