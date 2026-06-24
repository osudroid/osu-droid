package com.osudroid.scoring

import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.math.Interpolation
import com.osudroid.mods.*
import kotlin.math.exp
import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * Legacy score multiplier calculator. This is used to calculate score during version 4 to 5 database migration by
 * separating the total score with mod multipliers from the multiplier itself. This allows future mod score multiplier
 * changes to be applied without database migrations.
 *
 * @param difficulty The [BeatmapDifficulty] for the beatmap that the multipliers are calculated for. This must be the
 * [BeatmapDifficulty] **before** any [Mod] application.
 */
class LegacyScoreMultiplierCalculator @JvmOverloads constructor(private val difficulty: BeatmapDifficulty? = null) {
    private val multipliers = mutableMapOf<KClass<out Mod>, (Mod) -> Float>()

    init {
        // region Difficulty Reduction

        single<ModEasy>(0.5f)
        single<ModNoFail>(0.5f)
        single<ModReallyEasy>(0.5f)
        single<ModHalfTime> { rateAdjustMultiplier(trackRateMultiplier) }

        // endregion

        // region Difficulty Increase

        single<ModHardRock>(1.06f)
        single<ModPrecise>(1.06f)
        single<ModDoubleTime> { rateAdjustMultiplier(trackRateMultiplier) }
        single<ModNightCore> { rateAdjustMultiplier(trackRateMultiplier) }
        single<ModOldNightCore>(1.12f)
        single<ModHidden> { if (usesDefaultSettings) 1.06f else 1f }
        single<ModTraceable>(1.06f)
        single<ModFlashlight> { if (usesDefaultSettings) 1.12f else 1f }

        // endregion

        // region Conversion

        single<ModDifficultyAdjust> { difficultyAdjustMultiplier() }
        single<ModCustomSpeed> { rateAdjustMultiplier(trackRateMultiplier) }

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

    private inline fun <reified TMod : Mod> single(multiplier: Float) {
        multipliers[TMod::class] = { multiplier }
    }

    private inline fun <reified TMod : Mod> single(noinline multiplier: TMod.() -> Float) {
        multipliers[TMod::class] = { mod -> (mod as TMod).multiplier() }
    }

    /**
     * Calculates the multiplier to be applied to score with the given [mods].
     */
    fun calculateFor(mods: Iterable<Mod>): Float {
        val modsByType = mods.associateBy { it::class }

        if (modsByType.isEmpty()) {
            return 1f
        }

        var result = 1f

        for (type in modsByType.keys) {
            val multiplier = multipliers[type] ?: continue

            result *= multiplier(modsByType.getValue(type))
        }

        return result
    }

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
        private fun rateAdjustMultiplier(rate: Float) =
            if (rate > 1f) 1f + (rate - 1f) * 0.24f
            else 0.3f.pow((1f - rate) * 4f)

        private fun ModTimeRamp.timeRampMultiplier() =
            Interpolation.linear(
                rateAdjustMultiplier(initialRate),
                rateAdjustMultiplier(finalRate),
                ModTimeRamp.FINAL_RATE_PROGRESS.toFloat()
            )
    }
}
