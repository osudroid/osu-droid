@file:JvmName("ModConverter")

package com.rian.osu.utils

import com.rian.osu.mods.*
import okhttp3.internal.toImmutableMap
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import java.util.EnumSet

val modMap = mutableMapOf<GameMod, Mod>().apply {
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
}.toImmutableMap()

/**
 * Converts "legacy" [GameMod]s to new [Mod]s.
 *
 * @param mods The [GameMod]s to convert.
 * @return A [List] containing the new [Mod]s.
 */
@JvmOverloads
fun convertLegacyMods(mods: EnumSet<GameMod>, forceCS: Float = Float.NaN, forceAR: Float = Float.NaN,
                      forceOD: Float = Float.NaN, forceHP: Float = Float.NaN) = mutableListOf<Mod>().apply {
    mods.forEach {
        val convertedMod = modMap[it] ?:
            throw IllegalArgumentException("Cannot find the conversion of mod with short name \"${it.shortName}\"")

        add(convertedMod)
    }

    if (arrayOf(forceCS, forceAR, forceOD, forceHP).any { v -> !v.isNaN() }) {
        add(ModDifficultyAdjust(forceCS, forceAR, forceOD, forceHP))
    }
}