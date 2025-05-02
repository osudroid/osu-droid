package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator

/**
 * Represents the Easy mod.
 */
class ModEasy : Mod(), IModApplicableToDifficulty {
    override val name = "Easy"
    override val acronym = "EZ"
    override val description = "Larger circles, more forgiving HP drain, less accuracy required, and three lives!"
    override val type = ModType.DifficultyReduction
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + ModHardRock::class

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.5f

    override fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) = difficulty.run {
        difficultyCS = when (mode) {
            GameMode.Droid -> {
                val scale = CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficultyCS)

                CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(scale + 0.125f)
            }

            GameMode.Standard -> difficultyCS * ADJUST_RATIO
        }

        gameplayCS = when (mode) {
            GameMode.Droid -> {
                val scale = CircleSizeCalculator.droidCSToOldDroidGameplayScale(gameplayCS)

                CircleSizeCalculator.droidOldGameplayScaleToDroidCS(scale + 0.125f)
            }

            GameMode.Standard -> gameplayCS * ADJUST_RATIO
        }

        ar *= ADJUST_RATIO
        od *= ADJUST_RATIO
        hp *= ADJUST_RATIO
    }

    override fun deepCopy() = ModEasy()

    companion object {
        private const val ADJUST_RATIO = 0.5f
    }
}