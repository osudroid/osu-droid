package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.min

/**
 * Represents the Hard Rock mod.
 */
class ModHardRock : Mod(), IApplicableToDifficulty {
    override val droidString = "r"

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) = difficulty.run {
        cs = when (mode) {
            GameMode.Droid -> ++cs

            // CS uses a custom 1.3 ratio.
            GameMode.Standard -> min(cs * 1.3f, 10f)
        }

        ar = min(ar * ADJUST_RATIO, 10f)
        od = min(od * ADJUST_RATIO, 10f)
        hp = min(hp * ADJUST_RATIO, 10f)
    }

    companion object {
        private const val ADJUST_RATIO = 1.4f
    }
}