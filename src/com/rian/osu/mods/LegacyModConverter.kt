@file:Suppress("DEPRECATION")

package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.utils.ModHashMap
import com.rian.util.toFloatWithCommaSeparator
import kotlin.collections.forEach
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * A set of utilities to handle legacy mods storage conversion to the new storage format.
 */
object LegacyModConverter {
    /**
     * All [Mod]s that can be stored in the legacy mods format by their respective [GameMod].
     */
    val gameModMap: Map<GameMod, KClass<out Mod>> = mutableMapOf(
        GameMod.MOD_AUTO to ModAutoplay::class,
        GameMod.MOD_AUTOPILOT to ModAutopilot::class,
        GameMod.MOD_DOUBLETIME to ModDoubleTime::class,
        GameMod.MOD_EASY to ModEasy::class,
        GameMod.MOD_FLASHLIGHT to ModFlashlight::class,
        GameMod.MOD_HALFTIME to ModHalfTime::class,
        GameMod.MOD_HARDROCK to ModHardRock::class,
        GameMod.MOD_HIDDEN to ModHidden::class,
        GameMod.MOD_TRACEABLE to ModTraceable::class,
        GameMod.MOD_NIGHTCORE to ModNightCore::class,
        GameMod.MOD_NOFAIL to ModNoFail::class,
        GameMod.MOD_PERFECT to ModPerfect::class,
        GameMod.MOD_PRECISE to ModPrecise::class,
        GameMod.MOD_REALLYEASY to ModReallyEasy::class,
        GameMod.MOD_RELAX to ModRelax::class,
        GameMod.MOD_SCOREV2 to ModScoreV2::class,
        GameMod.MOD_SMALLCIRCLE to ModSmallCircle::class,
        GameMod.MOD_SUDDENDEATH to ModSuddenDeath::class
    )

    /**
     * All [Mod]s that can be stored in the legacy mods format by their respective encode character.
     */
    val legacyStorableMods: Map<Char, KClass<out Mod>> = mutableMapOf(
        'a' to ModAutoplay::class,
        'b' to ModTraceable::class,
        'c' to ModNightCore::class,
        'd' to ModDoubleTime::class,
        'e' to ModEasy::class,
        'f' to ModPerfect::class,
        'h' to ModHidden::class,
        'i' to ModFlashlight::class,
        'l' to ModReallyEasy::class,
        'm' to ModSmallCircle::class,
        'n' to ModNoFail::class,
        'p' to ModAutopilot::class,
        'r' to ModHardRock::class,
        's' to ModPrecise::class,
        't' to ModHalfTime::class,
        'u' to ModSuddenDeath::class,
        'v' to ModScoreV2::class,
        'x' to ModRelax::class
    )

    /**
     * Converts legacy [GameMod]s to new [Mod]s.
     *
     * @param mods The [GameMod]s to convert.
     * @param extraModString The extra mod string to parse.
     * @param difficulty The [BeatmapDifficulty] to use for [IMigratableMod] migrations. When `null`,
     * [IMigratableMod]s will not be migrated.
     * @return A [ModHashMap] containing the converted [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convert(mods: Iterable<GameMod>, extraModString: String, difficulty: BeatmapDifficulty? = null) =
        ModHashMap().apply {
            mods.forEach {
                val mod = gameModMap[it]?.createInstance() ?:
                throw IllegalArgumentException("Cannot find respective Mod class for $it.")

                if (mod is IMigratableMod && difficulty != null) {
                    put(mod.migrate(difficulty))
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
     * [IMigratableMod]s will not be migrated.
     * @return A [ModHashMap] containing the [Mod]s.
     */
    @JvmStatic
    @JvmOverloads
    fun convert(str: String?, difficulty: BeatmapDifficulty? = null) = ModHashMap().also {
        if (str.isNullOrEmpty()) return@also

        val data = str.split('|', limit = 2)

        for (c in data.getOrNull(0) ?: return@also) {
            val mod = legacyStorableMods[c]?.createInstance() ?: continue

            if (mod is IMigratableMod && difficulty != null) {
                it.put(mod.migrate(difficulty))
            } else {
                it.put(mod)
            }
        }

        parseExtraModString(it, data.getOrNull(1) ?: "")
    }

    private fun parseExtraModString(existingMods: ModHashMap, str: String) = existingMods.let {
        var customCS: Float? = null
        var customAR: Float? = null
        var customOD: Float? = null
        var customHP: Float? = null

        for (s in str.split('|')) {
            when {
                s.startsWith('x') && s.length == 5 -> it.put(ModCustomSpeed(s.substring(1).toFloatWithCommaSeparator()))

                s.startsWith("CS") -> customCS = s.substring(2).toFloatWithCommaSeparator()
                s.startsWith("AR") -> customAR = s.substring(2).toFloatWithCommaSeparator()
                s.startsWith("OD") -> customOD = s.substring(2).toFloatWithCommaSeparator()
                s.startsWith("HP") -> customHP = s.substring(2).toFloatWithCommaSeparator()

                s.startsWith("FLD") -> {
                    val followDelay = s.substring(3).toFloatWithCommaSeparator()
                    val flashlight = it.ofType<ModFlashlight>() ?: ModFlashlight().also { m -> it.put(m) }

                    flashlight.followDelay = followDelay
                }
            }
        }

        if (customCS != null || customAR != null || customOD != null || customHP != null) {
            // Do not pass difficulty statistics to the mod's constructor to prevent the mod's default values from being
            // changed, as they should stay null (we do not know the beatmap's difficulty values).
            it.put(ModDifficultyAdjust().apply {
                cs = customCS
                ar = customAR
                od = customOD
                hp = customHP
            })
        }
    }
}