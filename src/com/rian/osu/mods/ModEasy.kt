package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Easy mod.
 */
class ModEasy : Mod(), IApplicableToDifficulty {
    override val droidString = "e"

    override fun applyToDifficulty(difficulty: BeatmapDifficulty) = difficulty.run {
        cs *= ADJUST_RATIO
        ar *= ADJUST_RATIO
        od *= ADJUST_RATIO
        hp *= ADJUST_RATIO
    }

    companion object {
        private const val ADJUST_RATIO = 0.5f
    }
}