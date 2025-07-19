@file:Suppress("DEPRECATION")

package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.settings.NullableFloatModSetting
import com.rian.osu.utils.ModHashMap
import com.rian.util.toFloatWithCommaSeparator
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.isAccessible
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * A set of utilities to handle legacy mods storage conversion to the new storage format.
 */
object LegacyModConverter {
    /**
     * All [Mod]s that can be stored in the legacy mods format by their respective [GameMod].
     */
    val gameModMap = mutableMapOf<GameMod, KClass<out Mod>>().also {
        it[GameMod.MOD_AUTO] = ModAutoplay::class
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
    val legacyStorableMods = mutableMapOf<Char, KClass<out Mod>>().also {
        it['a'] = ModAutoplay::class
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
            val difficultyAdjust = ModDifficultyAdjust(customCS, customAR, customOD, customHP)

            // Set the default values to null since they are still unknown at this point.
            fun getDelegate(property: KProperty0<*>): NullableFloatModSetting {
                property.isAccessible = true
                return property.getDelegate() as NullableFloatModSetting
            }

            getDelegate(difficultyAdjust::cs).defaultValue = null
            getDelegate(difficultyAdjust::ar).defaultValue = null
            getDelegate(difficultyAdjust::od).defaultValue = null
            getDelegate(difficultyAdjust::hp).defaultValue = null

            it.put(difficultyAdjust)
        }
    }
}