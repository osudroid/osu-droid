package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the No Fail mod.
 */
class ModNoFail : Mod(), IModUserSelectable {
    override val encodeChar = 'n'
    override val name = "No Fail"
    override val acronym = "NF"
    override val textureNameSuffix = "nofail"
    override val enum = GameMod.MOD_NOFAIL
    override val isRanked = true
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModPerfect::class, ModSuddenDeath::class, ModAutopilot::class, ModRelax::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 0.5f

    override fun equals(other: Any?) = other === this || other is ModNoFail
    override fun hashCode() = super.hashCode()
    override fun deepCopy() = ModNoFail()
}