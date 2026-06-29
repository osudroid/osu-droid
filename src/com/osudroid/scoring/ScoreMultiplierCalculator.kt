package com.osudroid.scoring

import com.osudroid.GameMode
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.*
import com.osudroid.utils.ModUtils
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Current score multiplier calculator. This is used to calculate score after version 5 database migration.
 */
class ScoreMultiplierCalculator @JvmOverloads constructor(difficulty: BeatmapDifficulty? = null) :
    BaseScoreMultiplierCalculator<Double>(difficulty) {

    private var appliedDifficulty: BeatmapDifficulty? = null

    init {
        // region Difficulty Reduction

        single<ModEasy>(0.5)
        single<ModNoFail>(0.5)
        single<ModReallyEasy>(0.5)

        // endregion

        // region Difficulty Increase

        single<ModHardRock>(1.06)
        single<ModPrecise>(1.06)
        single<ModHidden> { hiddenMultiplier() }
        single<ModTraceable>(1.06)
        combination<ModFlashlight, ModFreezeFrame> { flashlight, _ -> 1 + (flashlight.flashlightMultiplier() - 1) / 2 }
        single<ModFlashlight> { flashlightMultiplier() }

        // endregion

        // region Conversion

        single<ModDifficultyAdjust> { difficultyAdjustMultiplier() }
        group<ModRateAdjust> { rateAdjustMultiplier() }

        // endregion

        // region Automation

        single<ModRelax>(1e-3)
        single<ModAutopilot>(1e-3)

        // endregion

        // region Fun

        single<ModWindUp> { timeRampMultiplier() }
        single<ModWindDown> { timeRampMultiplier() }
        single<ModApproachDifferent>(0.7)
        single<ModSynesthesia>(0.8)

        // endregion
    }

    override fun calculateFor(mods: Iterable<Mod>): Double {
        val appliedDifficulty = difficulty?.clone()

        if (appliedDifficulty != null) {
            ModUtils.applyModsToBeatmapDifficulty(appliedDifficulty, GameMode.Droid, mods)
            this.appliedDifficulty = appliedDifficulty
        }

        return super.calculateFor(mods)
    }

    override val defaultMultiplier = 1.0
    override fun multiply(a: Double, b: Double) = a * b

    private fun ModDifficultyAdjust.difficultyAdjustMultiplier(): Double {
        var multiplier = 1.0

        cs?.let { csValue ->
            val diff = (csValue - (difficulty?.difficultyCS ?: return@let)).toDouble()

            multiplier *=
                if (diff >= 0) 1 + 0.0075 * diff.pow(1.5)
                else 2 / (1 + exp(-0.5 * diff))
        }

        od?.let { odValue ->
            val diff = (odValue - (difficulty?.od ?: return@let)).toDouble()

            multiplier *=
                if (diff >= 0) 1.0 + 0.005 * diff.pow(1.3)
                else 2.0 / (1.0 + exp(-0.25 * diff))
        }

        return multiplier
    }

    companion object {
        // TODO: rebalancing, most of these are osu!lazer multipliers.
        private fun ModHidden.hiddenMultiplier(): Double {
            var value = 1.04

            if (onlyFadeApproachCircles) {
                value -= 0.02
            }

            return value
        }

        private fun ModFlashlight.flashlightMultiplier(): Double {
            // 1.12x base, reduced by 0.02 per 0.1 increase in flashlight size.
            val value = max(1.02, min(1.12, 1.12 - 0.2 * (sizeMultiplier.toDouble() - 1)))

            return if (!comboBasedSize) 1 + (value - 1) / 5 else value
        }

        private fun ModTimeRamp.timeRampMultiplier(): Double {
            val minSpeed = min(initialRate, finalRate)
            val maxSpeed = max(initialRate, finalRate)

            val minMultiplier =
                if (minSpeed < 1f) halfTimeMultiplier(minSpeed)
                else doubleTimeMultiplier(minSpeed)

            val maxMultiplier =
                if (maxSpeed < 1f) halfTimeMultiplier(maxSpeed)
                else doubleTimeMultiplier(maxSpeed)

            return 0.8 * minMultiplier + 0.2 * maxMultiplier
        }

        private fun Iterable<ModRateAdjust>.rateAdjustMultiplier(): Double {
            val combinedRate = fold(1f) { acc, mod -> acc * mod.trackRateMultiplier }

            return if (combinedRate < 1f) halfTimeMultiplier(combinedRate)
            else doubleTimeMultiplier(combinedRate)
        }

        private fun halfTimeMultiplier(speedChange: Float): Double {
            // 0.2x at 0.5x speed, +0.07x per 0.05x speed increment. Default HT (0.75x) = 0.55.
            return (speedChange * 20).toInt() / 20.0 * 1.4 - 0.5
        }

        private fun doubleTimeMultiplier(speedChange: Float): Double {
            // Floor to the nearest multiple of 0.1.
            val value = (speedChange * 10).toInt() / 10.0
            // 0.01 penalty for non-default rates. Linear from 1.0 to 1.46. Default DT (1.5x) = 1.23.
            val penalty = if (value != 1.5 && value != 1.0) 0.01 else 0.0

            return (value - 1) * 0.46 + 1 - penalty
        }
    }
}
