package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.min

/**
 * Represents the Hard Rock mod.
 */
class ModHardRock : Mod(), IApplicableToDifficulty {
    override val droidString = "r"

    override fun applyToDifficulty(difficulty: BeatmapDifficulty) = difficulty.run {
        // CS uses a custom 1.3 ratio.
        cs = min(cs * 1.3f, 10f)
        ar = min(ar * ADJUST_RATIO, 10f)
        od = min(od * ADJUST_RATIO, 10f)
        hp = min(hp * ADJUST_RATIO, 10f)
    }

    companion object {
        private const val ADJUST_RATIO = 1.4f
    }
}