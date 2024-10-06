package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Relax mod.
 */
class ModRelax : Mod(), IModUserSelectable {
    override val droidChar = 'x'
    override val acronym = "RX"
    override val textureNameSuffix = "relax"
    override val enum = GameMod.MOD_RELAX
    override val incompatibleMods = super.incompatibleMods + arrayOf(
        ModAuto::class, ModNoFail::class, ModAutopilot::class
    )

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1e-3f

    override fun equals(other: Any?) = other === this || other is ModRelax
    override fun hashCode() = super.hashCode()
}