package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Small Circle mod.
 */
class ModSmallCircle : Mod(), IModUserSelectable, ILegacyMod {
    override val droidChar = 'm'
    override val acronym = "SC"
    override val textureNameSuffix = "smallcircle"

    override fun migrate(difficulty: BeatmapDifficulty) = ModDifficultyAdjust(cs = difficulty.gameplayCS + 4)
}