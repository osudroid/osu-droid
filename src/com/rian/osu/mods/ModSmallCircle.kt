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

    override fun migrate(difficulty: BeatmapDifficulty) = ModDifficultyAdjust(cs = difficulty.gameplayCS + 4)

    override fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) {
        difficulty.gameplayCS += 4

        difficulty.difficultyCS = when (mode) {
            GameMode.Droid -> CircleSizeCalculator.droidCSToOldDroidDifficultyScale(difficulty.gameplayCS)
            GameMode.Standard -> difficulty.difficultyCS + 4
        }
    }

    override fun deepCopy() = ModSmallCircle()
}