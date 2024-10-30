@file:JvmName("MultiplayerConverter")

package com.reco1l.osu.multiplayer

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import ru.nsu.ccfit.zuev.osu.menu.ModMenu
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.util.*


// Mods

fun modsToReadable(
    mods: EnumSet<GameMod>?,
    speedMultiplier: Float,
    flFollowDelay: Float,
    customAR: Float?,
    customOD: Float?,
    customCS: Float?,
    customHP: Float?,
): String {

    if (mods.isNullOrEmpty()
        && speedMultiplier == 1f
        && flFollowDelay == ModMenu.DEFAULT_FL_FOLLOW_DELAY
        && customAR == null
        && customOD == null
        && customCS == null
        && customHP == null
    )
        return "None"

    return buildString {

        if (!mods.isNullOrEmpty()) {
            for (m in mods) when (m) {
                MOD_AUTO -> append("AU, ")
                MOD_RELAX -> append("RX, ")
                MOD_AUTOPILOT -> append("AP, ")
                MOD_EASY -> append("EZ, ")
                MOD_NOFAIL -> append("NF, ")
                MOD_HARDROCK -> append("HR, ")
                MOD_HIDDEN -> append("HD, ")
                MOD_FLASHLIGHT -> {
                    if (flFollowDelay == ModMenu.DEFAULT_FL_FOLLOW_DELAY) {
                        append("FL, ")
                    } else {
                        append("FL ${(flFollowDelay * 1000).toInt()}ms, ")
                    }
                }

                MOD_DOUBLETIME -> append("DT, ")
                MOD_NIGHTCORE -> append("NC, ")
                MOD_HALFTIME -> append("HT, ")
                MOD_PRECISE -> append("PR, ")
                MOD_REALLYEASY -> append("REZ, ")
                MOD_PERFECT -> append("PF, ")
                MOD_SUDDENDEATH -> append("SD, ")
                MOD_SCOREV2 -> append("SV2, ")
                else -> Unit
            }
        }

        if (speedMultiplier != 1f) {
            append("%.2fx, ".format(speedMultiplier))
        }

        if (customAR != null) {
            append("AR $customAR, ")
        }

        if (customOD != null) {
            append("OD $customOD, ")
        }

        if (customCS != null) {
            append("CS $customCS, ")
        }

        if (customHP != null) {
            append("HP $customHP, ")
        }

    }.substringBeforeLast(',')
}

fun stringToMods(data: String?): EnumSet<GameMod> {

    val mod = EnumSet.noneOf(GameMod::class.java)

    if (data.isNullOrEmpty()) {
        return mod
    }

    for (c in data) when (c) {
        'a' -> mod += MOD_AUTO
        'x' -> mod += MOD_RELAX
        'p' -> mod += MOD_AUTOPILOT
        'e' -> mod += MOD_EASY
        'n' -> mod += MOD_NOFAIL
        'r' -> mod += MOD_HARDROCK
        'h' -> mod += MOD_HIDDEN
        'i' -> mod += MOD_FLASHLIGHT
        'd' -> mod += MOD_DOUBLETIME
        'c' -> mod += MOD_NIGHTCORE
        't' -> mod += MOD_HALFTIME
        's' -> mod += MOD_PRECISE
        'l' -> mod += MOD_REALLYEASY
        'u' -> mod += MOD_SUDDENDEATH
        'f' -> mod += MOD_PERFECT
        'v' -> mod += MOD_SCOREV2
    }
    return mod
}

fun modsToString(mod: EnumSet<GameMod>) = buildString {

    for (m in mod) when (m) {
        MOD_AUTO -> append('a')
        MOD_RELAX -> append('x')
        MOD_AUTOPILOT -> append('p')
        MOD_EASY -> append('e')
        MOD_NOFAIL -> append('n')
        MOD_HARDROCK -> append('r')
        MOD_HIDDEN -> append('h')
        MOD_FLASHLIGHT -> append('i')
        MOD_DOUBLETIME -> append('d')
        MOD_NIGHTCORE -> append('c')
        MOD_HALFTIME -> append('t')
        MOD_PRECISE -> append('s')
        MOD_REALLYEASY -> append('l')
        MOD_PERFECT -> append('f')
        MOD_SUDDENDEATH -> append('u')
        MOD_SCOREV2 -> append('v')
        else -> Unit
    }
}


// Statistics

/**
 * Specifically made to handle `liveScoreData` event.
 */
fun jsonToScoreboardItem(json: JSONObject) = ScoreBoardItem().apply {

    userName = json.getString("username")
    playScore = json.getInt("score")
    maxCombo = json.getInt("combo")
    accuracy = json.getDouble("accuracy").toFloat()
    isAlive = json.getBoolean("isAlive")
}

/**
 * Specifically made to handle `scoreSubmission` event.
 */
fun jsonToStatistic(json: JSONObject) = StatisticV2().apply {

    playerName = json.getString("username")
    setForcedScore(json.getInt("score"))
    time = System.currentTimeMillis()
    setModFromString(if (json.isNull("modstring")) "" else json.getString("modstring"))
    scoreMaxCombo = json.optInt("maxCombo")
    hit300k = json.optInt("geki")
    hit300 = json.optInt("perfect")
    hit100k = json.optInt("katu")
    hit100 = json.optInt("good")
    hit50 = json.optInt("bad")
    misses = json.optInt("miss")
    isAlive = json.getBoolean("isAlive")
}
