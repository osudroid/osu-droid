package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator

/**
 * Represents the Easy mod.
 */
class ModEasy : Mod(), IModApplicableToDifficulty {
    override val droidString = "e"

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) = difficulty.run {
        cs = when (mode) {
            GameMode.Droid -> {
                val scale = CircleSizeCalculator.droidCSToDroidGameplayScale(cs)

                CircleSizeCalculator.droidGameplayScaleToDroidCS(scale + 0.125f)
            }

            GameMode.Standard -> cs * ADJUST_RATIO
        }

        ar *= ADJUST_RATIO
        od *= ADJUST_RATIO
        hp *= ADJUST_RATIO
    }

    companion object {
        private const val ADJUST_RATIO = 0.5f
    }
}