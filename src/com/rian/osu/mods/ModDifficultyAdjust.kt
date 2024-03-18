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

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty) = difficulty.let {
        it.cs = getValue(cs, it.cs)
        it.ar = getValue(ar, it.ar)
        it.od = getValue(od, it.od)
        it.hp = getValue(hp, it.hp)
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