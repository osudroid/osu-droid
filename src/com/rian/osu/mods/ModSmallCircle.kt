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
    override val textureNameSuffix = "smallcircle"

    override fun migrate(difficulty: BeatmapDifficulty) = ModDifficultyAdjust(cs = difficulty.gameplayCS + 4)

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) {
        difficulty.gameplayCS += 4

        difficulty.difficultyCS = when (mode) {
            GameMode.Droid -> CircleSizeCalculator.droidCSToDroidDifficultyScale(difficulty.gameplayCS)
            GameMode.Standard -> difficulty.difficultyCS + 4
        }
    }

    override fun equals(other: Any?) = other === this || other is ModSmallCircle
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModSmallCircle()
}