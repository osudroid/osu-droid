package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Small Circle mod.
 */
class ModSmallCircle : Mod(), IMigratableMod {
    override val name = "Small Circle"
    override val acronym = "SC"
    override val textureNameSuffix = "smallcircle"

    override fun migrate(difficulty: BeatmapDifficulty) = ModDifficultyAdjust(cs = difficulty.gameplayCS + 4)

    override fun equals(other: Any?) = other === this || other is ModSmallCircle
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModSmallCircle()
}