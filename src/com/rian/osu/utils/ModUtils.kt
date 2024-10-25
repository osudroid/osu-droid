package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*
import java.util.EnumSet
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
     * @return A [MutableList] containing the new [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convertLegacyMods(mods: EnumSet<GameMod>, forceCS: Float? = null, forceAR: Float? = null,
                          forceOD: Float? = null, forceHP: Float? = null) = mutableListOf<Mod>().apply {
        mods.forEach {
            val convertedMod = modMap[it] ?:
            throw IllegalArgumentException("Cannot find the conversion of mod with short name \"${it.shortName}\"")

            add(convertedMod)
        }

        if (arrayOf(forceCS, forceAR, forceOD, forceHP).any { v -> v != null }) {
            add(ModDifficultyAdjust(forceCS, forceAR, forceOD, forceHP))
        }
    }

    /**
     * Calculates the rate for the track with the selected [Mod]s.
     *
     * @param mods The list of selected [Mod]s.
     * @param oldStatistics Whether to enforce old statistics. Some [Mod]s behave differently with this flag. For
     * example, [ModNightCore] will apply a 1.39 rate multiplier instead of 1.5 when this is `true`.
     * **Never set this flag to `true` unless you know what you are doing.**
     * @return The rate with [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateRateWithMods(mods: Iterable<Mod>, oldStatistics: Boolean = false) = mods.fold(1f) {
        rate, mod -> (mod as? IModApplicableToTrackRate)?.applyToRate(rate, oldStatistics) ?: rate
    }

    /**
     * Applies the selected [Mod]s to a [BeatmapDifficulty].
     *
     * @param difficulty The [BeatmapDifficulty] to apply the [Mod]s to.
     * @param mode The [GameMode] to apply the [Mod]s for.
     * @param mods The selected [Mod]s.
     * @param customSpeedMultiplier The custom speed multiplier to apply.
     * @param withRateChange Whether to apply rate changes.
     * @param oldStatistics Whether to enforce old statistics. Some [Mod]s behave differently with this flag. For
     * example, [ModNightCore] will apply a 1.39 rate multiplier instead of 1.5 when this is `true`.
     * **Never set this flag to `true` unless you know what you are doing.**
     */
    @JvmStatic
    @JvmOverloads
    fun applyModsToBeatmapDifficulty(
        difficulty: BeatmapDifficulty,
        mode: GameMode,
        mods: Iterable<Mod>,
        customSpeedMultiplier: Float = 1f,
        withRateChange: Boolean = false,
        oldStatistics: Boolean = false
    ) {
        for (mod in mods) {
            if (mod is IModApplicableToDifficulty) {
                mod.applyToDifficulty(mode, difficulty)
            }
        }

        for (mod in mods) {
            if (mod is IModApplicableToDifficultyWithSettings) {
                mod.applyToDifficulty(mode, difficulty, mods, customSpeedMultiplier, oldStatistics)
            }
        }

        if (!withRateChange) {
            return
        }

        // Apply rate adjustments
        val totalSpeedMultiplier = calculateRateWithMods(mods, oldStatistics) * customSpeedMultiplier

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