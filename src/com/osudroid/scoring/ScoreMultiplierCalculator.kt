package com.osudroid.scoring

import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.*
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * Current score multiplier calculator. This is used to calculate score after version 5 database migration.
 */
class ScoreMultiplierCalculator @JvmOverloads constructor (private val difficulty: BeatmapDifficulty? = null) {

    private val singleMultipliers = mutableMapOf<KClass<out Mod>, (Mod) -> Double>()
    private val combinationMultipliers = mutableListOf<Pair<List<KClass<out Mod>>, (List<Mod>) -> Double>>()

    init {
        // region Difficulty Reduction

        single<ModEasy>(0.5)
        single<ModNoFail>(0.5)
        single<ModReallyEasy>(0.5)
        single<ModHalfTime> { halfTimeMultiplier(trackRateMultiplier.toDouble()) }

        // endregion

        // region Difficulty Increase

        single<ModHardRock>(1.06)
        single<ModPrecise>(1.06)
        single<ModDoubleTime> { doubleTimeMultiplier(trackRateMultiplier.toDouble()) }
        single<ModNightCore> { doubleTimeMultiplier(trackRateMultiplier.toDouble()) }
        single<ModOldNightCore> { doubleTimeMultiplier(trackRateMultiplier.toDouble()) }
        single<ModHidden> { hiddenMultiplier() }
        single<ModTraceable>(1.06)
        combination<ModFlashlight, ModFreezeFrame> { flashlight, _ -> 1.0 + (flashlight.flashlightMultiplier() - 1.0) / 2.0 }
        single<ModFlashlight> { flashlightMultiplier() }

        // endregion

        // region Conversion

        single<ModDifficultyAdjust> { difficultyAdjustMultiplier() }
        single<ModCustomSpeed> { rateMultiplier(trackRateMultiplier.toDouble()) }

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

    private inline fun <reified TMod : Mod> single(multiplier: Double) {
        singleMultipliers[TMod::class] = { multiplier }
    }

    private inline fun <reified TMod : Mod> single(noinline multiplier: TMod.() -> Double) {
        singleMultipliers[TMod::class] = { mod -> (mod as TMod).multiplier() }
    }

    private inline fun <reified T1 : Mod, reified T2 : Mod> combination(
        noinline multiplier: (T1, T2) -> Double
    ) {
        combinationMultipliers.add(listOf(T1::class, T2::class) to { mods -> multiplier(mods[0] as T1, mods[1] as T2) })
    }

    /**
     * Calculates the multiplier to be applied to score with the given [mods].
     */
    fun calculateFor(mods: Iterable<Mod>): Double {
        val modsByType = mods.associateBy { it::class }

        if (modsByType.isEmpty()) {
            return 1.0
        }

        val remaining = modsByType.keys.toMutableSet()
        var result = 1.0

        if (modsByType.size > 1) {
            for ((types, multiplier) in combinationMultipliers) {
                if (remaining.containsAll(types)) {
                    val instances = types.map { modsByType.getValue(it) }

                    result *= multiplier(instances)
                    remaining.removeAll(types.toSet())
                }
            }
        }

        for (type in remaining) {
            val multiplier = singleMultipliers[type] ?: continue

            result *= multiplier(modsByType.getValue(type))
        }

        return result
    }

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
            val value = max(1.02, min(1.12, 1.12 - 0.2 * (sizeMultiplier.toDouble() - 1.0)))

            return if (!comboBasedSize) 1.0 + (value - 1.0) / 5.0 else value
        }

        private fun ModTimeRamp.timeRampMultiplier(): Double {
            val minSpeed = minOf(initialRate, finalRate).toDouble()
            val maxSpeed = maxOf(initialRate, finalRate).toDouble()

            val minMultiplier =
                if (minSpeed < 1.0) halfTimeMultiplier(minSpeed)
                else doubleTimeMultiplier(minSpeed)

            val maxMultiplier =
                if (maxSpeed < 1.0) halfTimeMultiplier(maxSpeed)
                else doubleTimeMultiplier(maxSpeed)

            return 0.8 * minMultiplier + 0.2 * maxMultiplier
        }

        private fun rateMultiplier(rate: Double) =
            if (rate < 1.0) halfTimeMultiplier(rate) else doubleTimeMultiplier(rate)

        private fun halfTimeMultiplier(speedChange: Double): Double {
            // 0.2x at 0.5x speed, +0.07x per 0.05x speed increment. Default HT (0.75x) = 0.55.
            return (speedChange * 20).toInt() / 20.0 * 1.4 - 0.5
        }

        private fun doubleTimeMultiplier(speedChange: Double): Double {
            // Floor to the nearest multiple of 0.1.
            val value = (speedChange * 10).toInt() / 10.0
            // 0.01 penalty for non-default rates. Linear from 1.0 to 1.46. Default DT (1.5x) = 1.23.
            val penalty = if (value != 1.5 && value != 1.0) 0.01 else 0.0
            return (value - 1.0) * 0.46 + 1.0 - penalty
        }
    }
}
