package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the Speed-Up mod.
 */
class ModSpeedUp : ModClockRateAdjust(), ILegacyMod {
    override val droidChar = 'b'
    override val acronym = "SU"
    override val textureNameSuffix = "speedup"
    override val enum = GameMod.MOD_SPEEDUP
    override val trackRateMultiplier = 1.25f

    override fun migrate(difficulty: BeatmapDifficulty) = ModCustomSpeed(trackRateMultiplier)
}