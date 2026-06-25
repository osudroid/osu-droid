package com.osudroid.scoring

import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.math.Interpolation
import com.osudroid.mods.*
import kotlin.math.exp
import kotlin.math.pow

/**
 * Legacy score multiplier calculator. This is used to calculate score during version 4 to 5 database migration by
 * separating the total score with mod multipliers from the multiplier itself. This allows future mod score multiplier
 * changes to be applied without database migrations.
 */
class LegacyScoreMultiplierCalculator @JvmOverloads constructor(difficulty: BeatmapDifficulty? = null) :
    BaseScoreMultiplierCalculator<Float>(difficulty) {

    init {
        // region Difficulty Reduction

        single<ModEasy>(0.5f)
        single<ModNoFail>(0.5f)
        single<ModReallyEasy>(0.5f)

        // endregion

        // region Difficulty Increase

        single<ModHardRock>(1.06f)
        single<ModPrecise>(1.06f)
        single<ModHidden> { if (usesDefaultSettings) 1.06f else 1f }
        single<ModTraceable>(1.06f)
        single<ModFlashlight> { if (usesDefaultSettings) 1.12f else 1f }

        // endregion

        // region Conversion

        single<ModDifficultyAdjust> { difficultyAdjustMultiplier() }
        group<ModRateAdjust> { rateAdjustMultiplier() }

        // endregion

        // region Automation

        single<ModRelax>(1e-3f)
        single<ModAutopilot>(1e-3f)

        // endregion

        // region Fun

        single<ModWindUp> { timeRampMultiplier() }
        single<ModWindDown> { timeRampMultiplier() }
        single<ModSynesthesia>(0.8f)

        // endregion
    }

    override val defaultMultiplier = 1f
    override fun multiply(a: Float, b: Float) = a * b

    private fun ModDifficultyAdjust.difficultyAdjustMultiplier(): Float {
        var multiplier = 1f

        cs?.let { csValue ->
            val diff = csValue - (difficulty?.difficultyCS ?: return@let)

            multiplier *=
                if (diff >= 0f) 1f + 0.0075f * diff.pow(1.5f)
                else 2f / (1f + exp(-0.5f * diff))
        }

        od?.let { odValue ->
            val diff = odValue - (difficulty?.od ?: return@let)

            multiplier *=
                if (diff >= 0f) 1f + 0.005f * diff.pow(1.3f)
                else 2f / (1f + exp(-0.25f * diff))
        }

        return multiplier
    }

    companion object {
        private fun Iterable<ModRateAdjust>.rateAdjustMultiplier(): Float {
            val combinedRate = fold(1f) { acc, mod -> acc * mod.trackRateMultiplier }

            return rateMultiplier(combinedRate)
        }

        private fun rateMultiplier(rate: Float) =
            if (rate > 1f) 1f + (rate - 1f) * 0.24f
            else 0.3f.pow((1f - rate) * 4f)

        private fun ModTimeRamp.timeRampMultiplier() =
            Interpolation.linear(
                rateMultiplier(initialRate),
                rateMultiplier(finalRate),
                ModTimeRamp.FINAL_RATE_PROGRESS.toFloat()
            )
    }
}
