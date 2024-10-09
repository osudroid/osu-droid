package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Small Circle mod.
 */
class ModSmallCircle : Mod(), ILegacyMod {
    override val droidChar = 'm'
    override val acronym = "SC"
    override val textureNameSuffix = "smallcircle"
    override val enum = GameMod.MOD_SMALLCIRCLE

    override fun migrate(difficulty: BeatmapDifficulty) = ModDifficultyAdjust(cs = difficulty.gameplayCS + 4)
    override fun deepCopy() = ModSmallCircle()
}