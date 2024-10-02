package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlin.math.exp
import kotlin.math.pow

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
    override val acronym = "DA"

    override fun calculateScoreMultiplier(difficulty: BeatmapDifficulty): Float {
        var multiplier = 1f

        if (cs != null) {
            val diff = difficulty.difficultyCS - cs!!

            multiplier *=
                if (diff >= 0) 1 + 0.0075f * diff.pow(1.5f)
                else 2 / (1 + exp(-0.5f * diff))
        }

        if (od != null) {
            val diff = difficulty.od - od!!

            multiplier *=
                if (diff >= 0) 1 + 0.005f * diff.pow(1.3f)
                else 2 / (1 + exp(-0.25f * diff))
        }

        return multiplier
    }

    override fun applyToDifficulty(mode: GameMode, difficulty: BeatmapDifficulty, mods: List<Mod>, customSpeedMultiplier: Float) =
        difficulty.let {
            it.difficultyCS = getValue(cs, it.difficultyCS)
            it.gameplayCS = getValue(cs, it.gameplayCS)
            it.ar = getValue(ar, it.ar)
            it.od = getValue(od, it.od)
            it.hp = getValue(hp, it.hp)

            // Special case for force AR, where the AR value is kept constant with respect to game time.
            // This makes the player perceive the AR as is under all speed multipliers.
            if (ar != null) {
                val preempt = BeatmapDifficulty.difficultyRange(ar!!.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)
                val trackRate = calculateTrackRate(mods, customSpeedMultiplier)

                it.ar = BeatmapDifficulty.inverseDifficultyRange(preempt * trackRate, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN).toFloat()
            }
        }

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject, mods: List<Mod>, customSpeedMultiplier: Float) {
        // Special case for force AR, where the AR value is kept constant with respect to game time.
        // This makes the player perceive the fade in animation as is under all speed multipliers.
        if (ar == null) {
            return
        }

        val trackRate = calculateTrackRate(mods, customSpeedMultiplier)
        hitObject.timeFadeIn *= trackRate
    }

    private fun calculateTrackRate(mods: List<Mod>, customSpeedMultiplier: Float) =
        mods.filterIsInstance<IModApplicableToTrackRate>().fold(1f) { acc, mod -> acc * mod.trackRateMultiplier } * customSpeedMultiplier

    private fun getValue(value: Float?, fallback: Float) = value ?: fallback

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModDifficultyAdjust) {
            return false
        }

        return super.equals(other)
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