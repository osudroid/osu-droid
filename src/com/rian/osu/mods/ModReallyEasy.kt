package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator

/**
 * Represents the Really Easy mod.
 */
class ModReallyEasy : Mod(), IModApplicableToDifficultyWithSettings {
    override val droidString = "l"

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: List<Mod>, customSpeedMultiplier: Float) =
        difficulty.run {
            val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as ModDifficultyAdjust?

            if (difficultyAdjustMod?.ar == null) {
                if (mods.any { it is ModHardRock }) {
                    ar *= 2
                    ar -= 0.5f
                }

                ar -= 0.5f
                ar -= customSpeedMultiplier - 1
            }

            if (difficultyAdjustMod?.cs == null) {
                gameplayCS = when (mode) {
                    GameMode.Droid -> {
                        val scale = CircleSizeCalculator.droidCSToDroidGameplayScale(gameplayCS)

                        CircleSizeCalculator.droidGameplayScaleToDroidCS(scale + 0.125f)
                    }

                    GameMode.Standard -> gameplayCS * ADJUST_RATIO
                }
            }

            if (difficultyAdjustMod?.od == null) {
                od *= ADJUST_RATIO
            }

            if (difficultyAdjustMod?.hp == null) {
                hp *= ADJUST_RATIO
            }
        }

    companion object {
        private const val ADJUST_RATIO = 0.5f
    }
}