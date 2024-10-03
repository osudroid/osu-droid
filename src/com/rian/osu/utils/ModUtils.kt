package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * A set of utilities to handle [Mod] combinations.
 */
object ModUtils {
    private val modMap = mutableMapOf<GameMod, Mod>().apply {
        put(GameMod.MOD_AUTO, ModAuto())
        put(GameMod.MOD_AUTOPILOT, ModAutopilot())
        put(GameMod.MOD_DOUBLETIME, ModDoubleTime())
        put(GameMod.MOD_EASY, ModEasy())
        put(GameMod.MOD_FLASHLIGHT, ModFlashlight())
        put(GameMod.MOD_HALFTIME, ModHalfTime())
        put(GameMod.MOD_HARDROCK, ModHardRock())
        put(GameMod.MOD_HIDDEN, ModHidden())
        put(GameMod.MOD_NIGHTCORE, ModNightCore())
        put(GameMod.MOD_NOFAIL, ModNoFail())
        put(GameMod.MOD_PERFECT, ModPerfect())
        put(GameMod.MOD_PRECISE, ModPrecise())
        put(GameMod.MOD_REALLYEASY, ModReallyEasy())
        put(GameMod.MOD_RELAX, ModRelax())
        put(GameMod.MOD_SCOREV2, ModScoreV2())
        put(GameMod.MOD_SUDDENDEATH, ModSuddenDeath())
    }.toMap()

    /**
     * Converts "legacy" [GameMod]s to new [Mod]s.
     *
     * @param mods The [GameMod]s to convert.
     * @param forceCS The circle size to enforce.
     * @param forceAR The approach rate to enforce.
     * @param forceOD The overall difficulty to enforce.
     * @param forceHP The health drain to enforce.
     * @param customSpeedMultiplier The custom speed multiplier to use.
     * @return A [MutableSet] containing the new [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convertLegacyMods(mods: Iterable<GameMod>, forceCS: Float? = null, forceAR: Float? = null,
                          forceOD: Float? = null, forceHP: Float? = null,
                          customSpeedMultiplier: Float = 1f) = mutableSetOf<Mod>().apply {
        mods.forEach {
            val convertedMod = modMap[it] ?:
            throw IllegalArgumentException("Cannot find the conversion of mod with short name \"${it.shortName}\"")

            add(convertedMod)
        }

        if (forceCS != null || forceAR != null || forceOD != null || forceHP != null) {
            add(ModDifficultyAdjust(forceCS, forceAR, forceOD, forceHP))
        }

        if (customSpeedMultiplier != 1f) {
            add(ModCustomSpeed(customSpeedMultiplier))
        }
    }

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
     */
    @JvmStatic
    fun applyModsToBeatmapDifficulty(difficulty: BeatmapDifficulty, mode: GameMode, mods: Iterable<Mod>) {
        for (mod in mods) {
            if (mod is IModApplicableToDifficulty) {
                mod.applyToDifficulty(mode, difficulty)
            }
        }

        for (mod in mods) {
            if (mod is IModApplicableToDifficultyWithSettings) {
                mod.applyToDifficulty(mode, difficulty, mods)
            }
        }

        // Apply rate adjustments
        val trackRate = calculateRateWithMods(mods)

        val preempt = BeatmapDifficulty.difficultyRange(difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN) / trackRate
        difficulty.ar = BeatmapDifficulty.inverseDifficultyRange(preempt, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN).toFloat()

        val isPreciseMod = mods.any { it is ModPrecise }
        val hitWindow = if (isPreciseMod) PreciseDroidHitWindow(difficulty.od) else DroidHitWindow(difficulty.od)
        val greatWindow = hitWindow.greatWindow / trackRate

        difficulty.od =
            if (isPreciseMod) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
            else DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
    }
}