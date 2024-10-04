package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Speed-Up mod.
 */
class ModSpeedUp : ModClockRateAdjust(), ILegacyMod {
    override val droidChar = 'b'
    override val acronym = "SU"
    override val textureNameSuffix = "speedup"
    override val trackRateMultiplier = 1.25f

    override fun migrate(difficulty: BeatmapDifficulty) = ModCustomSpeed(trackRateMultiplier)
}