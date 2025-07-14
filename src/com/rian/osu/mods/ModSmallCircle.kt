package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.CircleSizeCalculator

/**
 * Represents the Small Circle mod.
 */
class ModSmallCircle : Mod(), IModApplicableToDifficulty, IMigratableMod {
    override val name = "Small Circle"
    override val acronym = "SC"
    override val description = "Who put ants in my beatmaps?"
    override val type = ModType.DifficultyIncrease

    override fun isCompatibleWith(other: Mod): Boolean {
        if (other is ModDifficultyAdjust) {
            return other.cs == null
        }

        return super.isCompatibleWith(other)
    }

    override fun migrate(difficulty: BeatmapDifficulty) = ModDifficultyAdjust(cs = difficulty.gameplayCS + 4)

    override fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) {
        difficulty.gameplayCS += 4

        difficulty.difficultyCS =
            if (mode == GameMode.Standard || adjustmentMods.none { it is ModReplayV6 }) difficulty.difficultyCS + 4
            else CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficulty.gameplayCS)
    }

    override fun deepCopy() = ModSmallCircle()
}