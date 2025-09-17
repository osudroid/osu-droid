package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator
import com.rian.osu.utils.ModUtils

/**
 * Represents the Really Easy mod.
 */
class ModReallyEasy : Mod(), IModApplicableToDifficultyWithMods {
    override val name = "Really Easy"
    override val acronym = "RE"
    override val description = "Everything just got easier..."
    override val type = ModType.DifficultyReduction
    override val scoreMultiplier = 0.5f

    override fun isCompatibleWith(other: Mod): Boolean {
        if (other is ModDifficultyAdjust) {
            return other.cs == null || other.ar == null || other.od == null || other.hp == null
        }

        return super.isCompatibleWith(other)
    }

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: Iterable<Mod>) =
        difficulty.run {
            val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as? ModDifficultyAdjust

            if (difficultyAdjustMod?.ar == null) {
                if (mods.any { it is ModEasy }) {
                    ar *= 2
                    ar -= 0.5f
                }

                val speedMultiplier = ModUtils.calculateRateWithMods(mods)

                ar -= 0.5f
                ar -= speedMultiplier - 1
            }

            if (difficultyAdjustMod?.cs == null) {
                if (mode == GameMode.Standard || mods.none { it is ModReplayV6 }) {
                    difficultyCS *= ADJUST_RATIO
                    gameplayCS *= ADJUST_RATIO
                } else {
                    val difficultyScale = CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficultyCS)
                    val gameplayScale = CircleSizeCalculator.droidCSToOldDroidGameplayScale(gameplayCS)

                    // The 0.125f scale that was added before replay version 7 was in screen pixels.
                    // We need it in osu! pixels.
                    val scaleAdjustment = 0.125f

                    difficultyCS = CircleSizeCalculator.droidOldDifficultyScaleToDroidCS(
                        difficultyScale + CircleSizeCalculator.droidOldDifficultyScaleScreenPixelsToOsuPixels(
                            scaleAdjustment
                        )
                    )

                    gameplayCS = CircleSizeCalculator.droidOldGameplayScaleToDroidCS(
                        gameplayScale + CircleSizeCalculator.droidOldGameplayScaleScreenPixelsToOsuPixels(
                            scaleAdjustment
                        )
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