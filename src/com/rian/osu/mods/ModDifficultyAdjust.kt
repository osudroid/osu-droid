package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.ModUtils

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
) : Mod(), IModApplicableToDifficultyWithSettings, IModApplicableToHitObjectWithSettings {
    override val droidString = ""

    override fun applyToDifficulty(
        mode: GameMode,
        difficulty: BeatmapDifficulty,
        mods: Iterable<Mod>,
        customSpeedMultiplier: Float,
        oldStatistics: Boolean
    ) = difficulty.let {
            it.difficultyCS = getValue(cs, it.difficultyCS)
            it.gameplayCS = getValue(cs, it.gameplayCS)
            it.ar = getValue(ar, it.ar)
            it.od = getValue(od, it.od)
            it.hp = getValue(hp, it.hp)

            // Special case for force AR, where the AR value is kept constant with respect to game time.
            // This makes the player perceive the AR as is under all speed multipliers.
            if (ar != null) {
                val preempt = BeatmapDifficulty.difficultyRange(ar!!.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)
                val trackRate = calculateTrackRate(mods, customSpeedMultiplier, oldStatistics)

                it.ar = BeatmapDifficulty.inverseDifficultyRange(preempt * trackRate, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN).toFloat()
            }
        }

    override fun applyToHitObject(
        mode: GameMode,
        hitObject: HitObject,
        mods: Iterable<Mod>,
        customSpeedMultiplier: Float,
        oldStatistics: Boolean
    ) {
        // Special case for force AR, where the AR value is kept constant with respect to game time.
        // This makes the player perceive the fade in animation as is under all speed multipliers.
        if (ar == null) {
            return
        }

        val trackRate = calculateTrackRate(mods, customSpeedMultiplier, oldStatistics)
        hitObject.timeFadeIn *= trackRate
    }

    private fun calculateTrackRate(mods: Iterable<Mod>, customSpeedMultiplier: Float, oldStatistics: Boolean) =
        ModUtils.calculateRateWithMods(mods, oldStatistics) * customSpeedMultiplier

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