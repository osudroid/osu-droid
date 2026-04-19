package com.rian.osu.difficulty.calculator

import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModRelax
import com.rian.osu.utils.ModHashMap
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class DroidRatingCalculator(private val mods: ModHashMap, private val totalHits: Int) {
    fun computeAimRating(aimDifficultyValue: Double) =
        if (ModAutopilot::class in mods) 0.0
        else aimDifficultyValue.pow(0.63) * 0.02275

    fun computeTapRating(tapDifficultyValue: Double) =
        if (ModRelax::class in mods) 0.0
        else calculateHarmonicDifficultyRating(tapDifficultyValue)

    fun computeRhythmRating(rhythmDifficultyValue: Double) =
        if (ModRelax::class in mods) 0.0
        else calculateHarmonicDifficultyRating(rhythmDifficultyValue)

    fun computeFlashlightRating(flashlightDifficultyValue: Double): Double {
        if (ModFlashlight::class !in mods) {
            return 0.0
        }

        var flashlightRating = calculateMechanicalDifficultyRating(flashlightDifficultyValue)

        if (ModRelax::class in mods) {
            flashlightRating *= 0.7
        } else if (ModAutopilot::class in mods) {
            flashlightRating *= 0.4
        }

        var ratingMultiplier = 1.0

        // Account for shorter maps having a higher ratio of 0 combo/100 combo flashlight radius.
        ratingMultiplier *= 0.7 + 0.1 * min(1.0, totalHits / 200.0) +
                if (totalHits > 200) 0.2 * min(1.0, (totalHits - 200) / 200.0) else 0.0

        return flashlightRating * sqrt(ratingMultiplier)
    }

    fun computeReadingRating(readingDifficultyValue: Double): Double {
        var readingRating = calculateHarmonicDifficultyRating(readingDifficultyValue)

        if (ModRelax::class in mods) {
            readingRating *= 0.6
        } else if (ModAutopilot::class in mods) {
            readingRating *= 0.3
        }

        return readingRating
    }

    companion object {
        private const val MECHANICAL_DIFFICULTY_MULTIPLIER = 0.18
        private const val COGNITION_DIFFICULTY_MULTIPLIER = 0.0675

        @JvmStatic
        fun calculateMechanicalDifficultyRating(difficultyValue: Double) = sqrt(difficultyValue) * MECHANICAL_DIFFICULTY_MULTIPLIER

        @JvmStatic
        fun calculateHarmonicDifficultyRating(difficultyValue: Double) = sqrt(difficultyValue) * COGNITION_DIFFICULTY_MULTIPLIER
    }
}