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
    override val scoreMultiplier = 0.5f
    override val incompatibleMods = super.incompatibleMods + ModHardRock::class

    override fun isCompatibleWith(other: Mod): Boolean {
        if (other is ModDifficultyAdjust) {
            return other.cs == null || other.ar == null || other.od == null || other.hp == null
        }

        return super.isCompatibleWith(other)
    }

    override fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) = difficulty.run {
        if (mode == GameMode.Standard || adjustmentMods.none { it is ModReplayV6 }) {
            difficultyCS *= ADJUST_RATIO
            gameplayCS *= ADJUST_RATIO
        } else {
            val difficultyScale = CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficultyCS)
            val gameplayScale = CircleSizeCalculator.droidCSToOldDroidGameplayScale(gameplayCS)

            // The 0.125f scale that was added before replay version 7 was in screen pixels. We need it in osu! pixels.
            val scaleAdjustment = 0.125f

            difficultyCS = CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(
                difficultyScale + CircleSizeCalculator.droidOldDifficultyScaleScreenPixelsToOsuPixels(scaleAdjustment)
            )

            gameplayCS = CircleSizeCalculator.droidOldGameplayScaleToDroidCS(
                gameplayScale + CircleSizeCalculator.droidOldGameplayScaleScreenPixelsToOsuPixels(scaleAdjustment)
            )
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