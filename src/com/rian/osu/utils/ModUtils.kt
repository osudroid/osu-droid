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
    private val gameModMap = mutableMapOf<GameMod, KClass<out Mod>>().also {
        it[GameMod.MOD_AUTO] = ModAuto::class
        it[GameMod.MOD_AUTOPILOT] = ModAutopilot::class
        it[GameMod.MOD_DOUBLETIME] = ModDoubleTime::class
        it[GameMod.MOD_EASY] = ModEasy::class
        it[GameMod.MOD_FLASHLIGHT] = ModFlashlight::class
        it[GameMod.MOD_HALFTIME] = ModHalfTime::class
        it[GameMod.MOD_HARDROCK] = ModHardRock::class
        it[GameMod.MOD_HIDDEN] = ModHidden::class
        it[GameMod.MOD_NIGHTCORE] = ModNightCore::class
        it[GameMod.MOD_NOFAIL] = ModNoFail::class
        it[GameMod.MOD_PERFECT] = ModPerfect::class
        it[GameMod.MOD_PRECISE] = ModPrecise::class
        it[GameMod.MOD_REALLYEASY] = ModReallyEasy::class
        it[GameMod.MOD_RELAX] = ModRelax::class
        it[GameMod.MOD_SCOREV2] = ModScoreV2::class
        it[GameMod.MOD_SMALLCIRCLE] = ModSmallCircle::class
        it[GameMod.MOD_SPEEDUP] = ModSpeedUp::class
        it[GameMod.MOD_SUDDENDEATH] = ModSuddenDeath::class
    }.toMap()

    /**
     * All [Mod]s that are considered legacy.
     */
    private val legacyMods = mutableMapOf<Char, ILegacyMod>().also {
        val legacyMods = arrayOf(ModSmallCircle(), ModSpeedUp())

        for (mod in legacyMods) {
            it[mod.droidChar] = mod
        }
    }.toMap()

    /**
     * All [Mod]s that can be selected by the user.
     */
    private val playableMods = mutableMapOf<Char, KClass<out Mod>>().also {
        val playableMods = arrayOf(
            ModAuto(), ModAutopilot(), ModDoubleTime(), ModEasy(), ModFlashlight(), ModHalfTime(),
            ModHardRock(), ModHidden(), ModNightCore(), ModNoFail(), ModPerfect(), ModPrecise(),
            ModReallyEasy(), ModRelax(), ModScoreV2(), ModSuddenDeath()
        )

        for (mod in playableMods) {
            it[mod.droidChar] = mod::class
        }
    }.toMap()

    /**
     * Converts "legacy" [GameMod]s to new [Mod]s.
     *
     * @param mods The [GameMod]s to convert.
     * @param extraModString The extra mod string to parse.
     * @param difficulty The [BeatmapDifficulty] to use for [ILegacyMod] migrations. When `null`, [ILegacyMod]s will not be added and migrated.
     */
    @JvmStatic
    @JvmOverloads
    fun convertLegacyMods(mods: Iterable<GameMod>, extraModString: String, difficulty: BeatmapDifficulty? = null) =
        ModHashSet().apply {
            mods.forEach {
                val convertedMod = gameModMap[it] ?:
                throw IllegalArgumentException("Cannot find the conversion of mod with short name \"${it.shortName}\"")

                val mod = convertedMod.createInstance()

                if (mod is ILegacyMod && difficulty != null) {
                    add(mod.migrate(difficulty))
                } else {
                    add(mod)
                }
            }

            parseExtraModString(this, extraModString)
        }

    /**
     * Converts a mod string to a [ModHashSet].
     *
     * @param str The mod string to convert. A `null` would return an empty [ModHashSet].
     * @param difficulty The [BeatmapDifficulty] to use for [ILegacyMod] migrations. When `null`, [ILegacyMod]s will not be added and migrated.
     * @return A [ModHashSet] containing the [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convertModString(str: String?, difficulty: BeatmapDifficulty? = null) = ModHashSet().also {
        if (str.isNullOrEmpty()) return@also

        val data = str.split('|', limit = 2)

        for (c in data.getOrNull(0) ?: return@also) when {
            c in playableMods -> it.add(playableMods[c]!!.createInstance())
            difficulty != null && c in legacyMods -> it.add(legacyMods[c]!!.migrate(difficulty))
        }

        parseExtraModString(it, data.getOrNull(1) ?: "")
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

    private fun parseExtraModString(existingMods: ModHashSet, str: String) = existingMods.let {
        var customCS: Float? = null
        var customAR: Float? = null
        var customOD: Float? = null
        var customHP: Float? = null

        for (s in str.split('|')) {
            when {
                s.startsWith('x') && s.length == 5 -> it.add(ModCustomSpeed(s.substring(1).toFloat()))

                s.startsWith("CS") -> customCS = s.substring(2).toFloat()
                s.startsWith("AR") -> customAR = s.substring(2).toFloat()
                s.startsWith("OD") -> customOD = s.substring(2).toFloat()
                s.startsWith("HP") -> customHP = s.substring(2).toFloat()

                s.startsWith("FLD") -> {
                    val followDelay = s.substring(3).toFloat()
                    val flashlightMod = it.find { m -> m is ModFlashlight } as ModFlashlight?

                    if (flashlightMod != null) {
                        flashlightMod.followDelay = followDelay
                    } else {
                        it.add(ModFlashlight().also { m -> m.followDelay = followDelay })
                    }
                }
            }
        }

        if (customCS != null || customAR != null || customOD != null || customHP != null) {
            it.add(ModDifficultyAdjust(customCS, customAR, customOD, customHP))
        }
    }
}