package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Really Easy mod.
 */
class ModReallyEasy : Mod(), IApplicableToDifficultyWithSettings {
    override val droidString = "l"

    override fun applyToDifficulty(difficulty: BeatmapDifficulty, mods: List<Mod>, customSpeedMultiplier: Float) =
        difficulty.run {
            val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as ModDifficultyAdjust?

            if (difficultyAdjustMod == null || difficultyAdjustMod.ar.isNaN()) {
                if (mods.any { it is ModHardRock }) {
                    ar *= 2
                    ar -= 0.5f
                }

                ar -= 0.5f
                ar -= customSpeedMultiplier - 1
            }

            if (difficultyAdjustMod == null || difficultyAdjustMod.cs.isNaN()) {
                cs *= ADJUST_RATIO
            }

            if (difficultyAdjustMod == null || difficultyAdjustMod.od.isNaN()) {
                od *= ADJUST_RATIO
            }

            if (difficultyAdjustMod == null || difficultyAdjustMod.hp.isNaN()) {
                hp *= ADJUST_RATIO
            }
        }

    companion object {
        private const val ADJUST_RATIO = 0.5f
    }
}