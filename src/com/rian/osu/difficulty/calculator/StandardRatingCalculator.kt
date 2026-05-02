package com.rian.osu.difficulty.calculator

import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModRelax
import com.rian.osu.utils.ModHashMap
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class StandardRatingCalculator(
    private val mods: ModHashMap,
    private val totalHits: Int,
    private val overallDifficulty: Double
) {
    fun computeAimRating(aimDifficultyValue: Double): Double {
        if (ModAutopilot::class in mods) {
            return 0.0
        }

        val aimRating = aimDifficultyValue.pow(0.63) * 0.02275
        var ratingMultiplier = 1.0

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

        return speedRating
    }

    fun computeFlashlightRating(flashlightDifficultyValue: Double): Double {
        if (ModFlashlight::class !in mods) {
            return 0.0
        }

        var flashlightRating = calculateDifficultyRating(flashlightDifficultyValue)

        if (ModRelax::class in mods) {
            flashlightRating *= 0.7
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

    fun computeReadingRating(readingDifficultyValue: Double): Double {
        var readingRating = calculateDifficultyRating(readingDifficultyValue)

        if (ModRelax::class in mods) {
            readingRating *= 0.6
        } else if (ModAutopilot::class in mods) {
            readingRating *= 0.3
        }

        var ratingMultiplier = 1.0
        ratingMultiplier *= 0.75 + max(0.0, overallDifficulty).pow(2.2) / 800

        return readingRating * cbrt(ratingMultiplier)
    }

    companion object {
        private const val DIFFICULTY_MULTIPLIER = 0.0675

        @JvmStatic
        fun calculateDifficultyRating(difficultyValue: Double) = sqrt(difficultyValue) * DIFFICULTY_MULTIPLIER
    }
}