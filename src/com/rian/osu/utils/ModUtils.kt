@file:Suppress("DEPRECATION")

package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * A set of utilities to handle [Mod] combinations.
 */
object ModUtils {
    /**
     * All [Mod]s that can be stored in the legacy mods format by their respective [GameMod].
     */
    private val gameModMap = mutableMapOf<GameMod, KClass<out Mod>>().also {
        it[GameMod.MOD_AUTO] = ModAuto::class
        it[GameMod.MOD_AUTOPILOT] = ModAutopilot::class
        it[GameMod.MOD_DOUBLETIME] = ModDoubleTime::class
        it[GameMod.MOD_EASY] = ModEasy::class
        it[GameMod.MOD_FLASHLIGHT] = ModFlashlight::class
        it[GameMod.MOD_HALFTIME] = ModHalfTime::class
        it[GameMod.MOD_HARDROCK] = ModHardRock::class
        it[GameMod.MOD_HIDDEN] = ModHidden::class
        it[GameMod.MOD_TRACEABLE] = ModTraceable::class
        it[GameMod.MOD_NIGHTCORE] = ModNightCore::class
        it[GameMod.MOD_NOFAIL] = ModNoFail::class
        it[GameMod.MOD_PERFECT] = ModPerfect::class
        it[GameMod.MOD_PRECISE] = ModPrecise::class
        it[GameMod.MOD_REALLYEASY] = ModReallyEasy::class
        it[GameMod.MOD_RELAX] = ModRelax::class
        it[GameMod.MOD_SCOREV2] = ModScoreV2::class
        it[GameMod.MOD_SMALLCIRCLE] = ModSmallCircle::class
        it[GameMod.MOD_SUDDENDEATH] = ModSuddenDeath::class
    }

    /**
     * All [Mod]s that can be stored in the legacy mods format by their respective encode character.
     */
    // TODO: this should no longer be required after serialization is actually implemented
    private val legacyStorableMods = mutableMapOf<Char, KClass<out Mod>>().also {
        it['a'] = ModAuto::class
        it['b'] = ModTraceable::class
        it['c'] = ModNightCore::class
        it['d'] = ModDoubleTime::class
        it['e'] = ModEasy::class
        it['f'] = ModPerfect::class
        it['h'] = ModHidden::class
        it['i'] = ModFlashlight::class
        it['l'] = ModReallyEasy::class
        it['m'] = ModSmallCircle::class
        it['n'] = ModNoFail::class
        it['p'] = ModAutopilot::class
        it['r'] = ModHardRock::class
        it['s'] = ModPrecise::class
        it['t'] = ModHalfTime::class
        it['u'] = ModSuddenDeath::class
        it['v'] = ModScoreV2::class
        it['x'] = ModRelax::class
    }

    /**
     * Converts legacy [GameMod]s to new [Mod]s.
     *
     * @param mods The [GameMod]s to convert.
     * @param extraModString The extra mod string to parse.
     * @param difficulty The [BeatmapDifficulty] to use for [IMigratableMod] migrations. When `null`,
     * [IMigratableMod]s will not be added and migrated.
     * @return A [ModHashMap] containing the converted [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convertLegacyMods(mods: Iterable<GameMod>, extraModString: String, difficulty: BeatmapDifficulty? = null) =
        ModHashMap().apply {
            mods.forEach {
                val mod = gameModMap[it]?.createInstance() ?:
                    throw IllegalArgumentException("Cannot find respective Mod class for $it.")

                if (mod is IMigratableMod) {
                    if (difficulty != null) {
                        put(mod.migrate(difficulty))
                    }
                } else {
                    put(mod)
                }
            }

            parseExtraModString(this, extraModString)
        }

    /**
     * Converts a mod string to a [ModHashMap].
     *
     * @param str The mod string to convert. A `null` would return an empty [ModHashMap].
     * @param difficulty The [BeatmapDifficulty] to use for [IMigratableMod] migrations. When `null`,
     * [IMigratableMod]s will not be added and migrated.
     * @return A [ModHashMap] containing the [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convertModString(str: String?, difficulty: BeatmapDifficulty? = null) = ModHashMap().also {
        if (str.isNullOrEmpty()) return@also

        val data = str.split('|', limit = 2)

        for (c in data.getOrNull(0) ?: return@also) {
            val mod = legacyStorableMods[c]?.createInstance() ?: continue

            if (mod is IMigratableMod) {
                if (difficulty != null) {
                    it.put(mod.migrate(difficulty))
                }
            } else {
                it.put(mod)
            }
        }

        parseExtraModString(it, data.getOrNull(1) ?: "")
    }

    /**
     * Calculates the playback rate for the track with the selected [Mod]s at [time].
     *
     * @param mods The list of selected [Mod]s.
     * @param time The time at which the playback rate is queried, in milliseconds. Defaults to 0.
     * @return The rate with [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun calculateRateWithMods(mods: Iterable<Mod>, time: Double = 0.0) = mods.fold(1f) { rate, mod ->
        (mod as? IModApplicableToTrackRate)?.applyToRate(time, rate) ?: rate
    }

    /**
     * Calculates the playback rate for the track with the selected [IModApplicableToTrackRate]s at [time].
     *
     * @param mods The list of selected [IModApplicableToTrackRate]s.
     * @param time The time at which the playback rate is queried, in milliseconds. Defaults to 0.
     * @return The rate with [IModApplicableToTrackRate]s.
     */
    @JvmStatic
    @JvmOverloads
    @JvmName("calculateRateWithTrackRateMods")
    fun calculateRateWithMods(mods: Iterable<IModApplicableToTrackRate>, time: Double = 0.0) =
        mods.fold(1f) { rate, mod ->
            mod.applyToRate(time, rate)
        }

    /**
     * Applies the selected [Mod]s to a [BeatmapDifficulty].
     *
     * @param difficulty The [BeatmapDifficulty] to apply the [Mod]s to.
     * @param mode The [GameMode] to apply the [Mod]s for.
     * @param mods The selected [Mod]s.
     * @param withRateChange Whether to apply rate changes to the [BeatmapDifficulty].
     */
    @JvmStatic
    @JvmOverloads
    fun applyModsToBeatmapDifficulty(
        difficulty: BeatmapDifficulty,
        mode: GameMode,
        mods: Iterable<Mod>,
        withRateChange: Boolean = false
    ) {
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

        if (!withRateChange) {
            return
        }

        // Apply rate adjustments
        val trackRate = calculateRateWithMods(mods, Double.POSITIVE_INFINITY)

        val preempt = BeatmapDifficulty.difficultyRange(
            difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ) / trackRate

        difficulty.ar = BeatmapDifficulty.inverseDifficultyRange(
            preempt, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ).toFloat()

        val isPreciseMod = mods.any { it is ModPrecise }
        val hitWindow = if (isPreciseMod) PreciseDroidHitWindow(difficulty.od) else DroidHitWindow(difficulty.od)
        val greatWindow = hitWindow.greatWindow / trackRate

        difficulty.od =
            if (isPreciseMod) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
            else DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow)
    }

    private fun parseExtraModString(existingMods: ModHashMap, str: String) = existingMods.let {
        var customCS: Float? = null
        var customAR: Float? = null
        var customOD: Float? = null
        var customHP: Float? = null

        for (s in str.split('|')) {
            when {
                s.startsWith('x') && s.length == 5 -> it.put(ModCustomSpeed(s.substring(1).toFloat()))

                s.startsWith("CS") -> customCS = s.substring(2).toFloat()
                s.startsWith("AR") -> customAR = s.substring(2).toFloat()
                s.startsWith("OD") -> customOD = s.substring(2).toFloat()
                s.startsWith("HP") -> customHP = s.substring(2).toFloat()

                s.startsWith("FLD") -> {
                    val followDelay = s.substring(3).toFloat()
                    val flashlight = it.ofType<ModFlashlight>() ?: ModFlashlight().also { m -> it.put(m) }

                    flashlight.followDelay = followDelay
                }
            }
        }

        if (customCS != null || customAR != null || customOD != null || customHP != null) {
            it.put(ModDifficultyAdjust(customCS, customAR, customOD, customHP))
        }
    }
}