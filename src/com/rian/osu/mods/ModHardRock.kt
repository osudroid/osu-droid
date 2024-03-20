package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator
import kotlin.math.min

/**
 * Represents the Hard Rock mod.
 */
class ModHardRock : Mod(), IApplicableToDifficulty {
    override val droidString = "r"

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) = difficulty.run {
        cs = when (mode) {
            GameMode.Droid -> {
                val scale = CircleSizeCalculator.droidCSToDroidScale(cs)

                CircleSizeCalculator.droidScaleToDroidCS(scale - 0.125f)
            }

            // CS uses a custom 1.3 ratio.
            GameMode.Standard -> applySetting(cs, 1.3f)
        }

        ar = applySetting(ar)
        od = applySetting(od)
        hp = applySetting(hp)
    }

    private fun applySetting(value: Float, ratio: Float = ADJUST_RATIO) = min(value * ratio, 10f)

    companion object {
        private const val ADJUST_RATIO = 1.4f
    }
}