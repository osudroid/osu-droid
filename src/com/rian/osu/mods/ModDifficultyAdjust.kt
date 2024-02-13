package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty

/**
 * Represents the Difficulty Adjust mod, serves as a container for force difficulty statistics.
 */
class ModDifficultyAdjust(
    /**
     * The circle size to enforce.
     */
    @JvmField
    var cs: Float = Float.NaN,

    /**
     * The approach rate to enforce.
     */
    @JvmField
    var ar: Float = Float.NaN,

    /**
     * The overall difficulty to enforce.
     */
    @JvmField
    var od: Float = Float.NaN,

    /**
     * The health drain rate to enforce.
     */
    @JvmField
    var hp: Float = Float.NaN
) : Mod(), IApplicableToDifficulty {
    override val droidString = ""

    override fun applyToDifficulty(difficulty: BeatmapDifficulty) = difficulty.let {
        if (!cs.isNaN()) {
            it.cs = cs
        }

        if (!ar.isNaN()) {
            it.ar = ar
        }

        if (!od.isNaN()) {
            it.od = od
        }

        if (!hp.isNaN()) {
            it.hp = hp
        }

        Unit
    }

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