package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*

/**
 * A set of utilities to handle [Mod] combinations.
 */
object ModUtils {
    /**
     * Calculates the rate for the track with the selected [Mod]s.
     *
     * @param mods The list of selected [Mod]s.
     * @return The rate with [Mod]s.
     */
    @JvmStatic
    fun calculateRateWithMods(mods: Iterable<Mod>) = mods.fold(1f) {
        rate, mod -> rate * (if (mod is IModApplicableToTrackRate) mod.trackRateMultiplier else 1f)
    }

    /**
     * Applies the selected [Mod]s to a [BeatmapDifficulty].
     *
     * @param difficulty The [BeatmapDifficulty] to apply the [Mod]s to.
     * @param mode The [GameMode] to apply the [Mod]s for.
     * @param mods The selected [Mod]s.
     * @param customSpeedMultiplier The custom speed multiplier to apply.
     */
    @JvmStatic
    @JvmOverloads
    fun applyModsToBeatmapDifficulty(difficulty: BeatmapDifficulty, mode: GameMode, mods: Iterable<Mod>, customSpeedMultiplier: Float = 1f) {
        for (mod in mods) {
            if (mod is IModApplicableToDifficulty) {
                mod.applyToDifficulty(mode, difficulty)
            }
        }

        for (mod in mods) {
            if (mod is IModApplicableToDifficultyWithSettings) {
                mod.applyToDifficulty(mode, difficulty, mods, customSpeedMultiplier)
            }
        }

        // Apply rate adjustments
        val totalSpeedMultiplier = calculateRateWithMods(mods) * customSpeedMultiplier

        val preempt = BeatmapDifficulty.difficultyRange(difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN) / totalSpeedMultiplier
        difficulty.ar = BeatmapDifficulty.inverseDifficultyRange(preempt, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN).toFloat()

        val isPreciseMod = mods.any { it is ModPrecise }
        val hitWindow = if (isPreciseMod) PreciseDroidHitWindow(difficulty.od) else DroidHitWindow(difficulty.od)
        val greatWindow = hitWindow.greatWindow / totalSpeedMultiplier

        difficulty.od =
            if (isPreciseMod) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
            else DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
    }
}