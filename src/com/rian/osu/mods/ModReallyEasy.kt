package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator

/**
 * Represents the Really Easy mod.
 */
class ModReallyEasy : Mod(), IModApplicableToDifficultyWithMods {
    override val name = "Really Easy"
    override val acronym = "RE"
    override val description = "Everything just got easier..."
    override val type = ModType.DifficultyReduction

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.5f

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: Iterable<Mod>) =
        difficulty.run {
            val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as? ModDifficultyAdjust

            if (difficultyAdjustMod?.ar == null) {
                if (mods.any { it is ModEasy }) {
                    ar *= 2
                    ar -= 0.5f
                }

                val customSpeedMultiplier =
                    (mods.find { it is ModCustomSpeed } as? ModCustomSpeed)?.trackRateMultiplier ?: 1f

                ar -= 0.5f
                ar -= customSpeedMultiplier - 1
            }

            if (difficultyAdjustMod?.cs == null) {
                if (mode == GameMode.Standard || mods.none { it is ModReplayV6 }) {
                    difficultyCS *= ADJUST_RATIO
                    gameplayCS *= ADJUST_RATIO
                } else {
                    val difficultyScale = CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficultyCS)
                    val gameplayScale = CircleSizeCalculator.droidCSToOldDroidGameplayScale(gameplayCS)

                    difficultyCS = CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(difficultyScale + 0.125f)

                    // In gameplay, the 0.125f scale is in real screen pixels.
                    gameplayCS = CircleSizeCalculator.droidOldGameplayScaleToDroidCS(
                        gameplayScale + CircleSizeCalculator.droidOldScaleScreenPixelsToOsuPixels(0.125f)
                    )
                }
            }

            if (difficultyAdjustMod?.od == null) {
                od *= ADJUST_RATIO
            }

            if (difficultyAdjustMod?.hp == null) {
                hp *= ADJUST_RATIO
            }
        }

    override fun deepCopy() = ModReallyEasy()

    companion object {
        private const val ADJUST_RATIO = 0.5f
    }
}