package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Difficulty Adjust mod, serves as a container for force difficulty statistics.
 */
class ModDifficultyAdjust(
    /**
     * The circle size to enforce.
     */
    @JvmField
    var cs: Float? = null,

    /**
     * The approach rate to enforce.
     */
    @JvmField
    var ar: Float? = null,

    /**
     * The overall difficulty to enforce.
     */
    @JvmField
    var od: Float? = null,

    /**
     * The health drain rate to enforce.
     */
    @JvmField
    var hp: Float? = null
) : Mod(), IApplicableToDifficulty {
    override val droidString = ""

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) = difficulty.let { d ->
        d.cs = getValue(cs, d.cs)
        d.ar = getValue(ar, d.ar)
        d.od = getValue(od, d.od)
        d.hp = getValue(hp, d.hp)
    }

    private fun getValue(value: Float?, fallback: Float) = value ?: fallback

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModDifficultyAdjust) {
            return false
        }

        return super.equals(other) && cs == other.cs && ar == other.ar && od == other.od && hp == other.hp
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + cs.hashCode()
        result = 31 * result + ar.hashCode()
        result = 31 * result + od.hashCode()
        result = 31 * result + hp.hashCode()

        return result
    }
}