@file:JvmName("MultiplayerConverter")

package com.reco1l.legacy.data

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.util.*


// Mods

fun modsToReadable(mods: String?): String
{
    if (mods.isNullOrEmpty()) return "None"

    return buildString {

        for (c in mods) when (c)
        {
            'a' -> append("AU, ")
            'x' -> append("RX, ")
            'p' -> append("AP, ")
            'e' -> append("EZ, ")
            'n' -> append("NF, ")
            'r' -> append("HR, ")
            'h' -> append("HD, ")
            'i' -> append("FL, ")
            'd' -> append("DT, ")
            'c' -> append("NC, ")
            't' -> append("HT, ")
            's' -> append("PR, ")
            'l' -> append("REZ, ")
            'm' -> append("SC, ")
            'u' -> append("SD, ")
            'f' -> append("PF, ")
            'b' -> append("SU, ")
            'v' -> append("SV2, ")
        }

    }.substringBeforeLast(',')
}

fun stringToMods(data: String?): EnumSet<GameMod>
{
    val mod = EnumSet.noneOf(GameMod::class.java)

    if (data.isNullOrEmpty()) return mod

    for (c in data) when (c)
    {
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
        'm' -> mod += MOD_SMALLCIRCLE
        'l' -> mod += MOD_REALLYEASY
        'u' -> mod += MOD_SUDDENDEATH
        'f' -> mod += MOD_PERFECT
        'v' -> mod += MOD_SCOREV2
    }
    return mod
}

fun modsToString(mod: EnumSet<GameMod>) = buildString {

    for (m in mod) when (m)
    {
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
        MOD_SMALLCIRCLE -> append('m')
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
}

/**
 * Specifically made to handle `scoreSubmission` event.
 */
fun jsonToStatistic(json: JSONObject) = StatisticV2().apply {

    playerName = json.getString("username")
    totalScore = json.getInt("score")
    time = System.currentTimeMillis()
    setModFromString(if (json.isNull("modstring")) "" else json.getString("modstring"))
    maxCombo = json.optInt("maxCombo")
    hit300k = json.optInt("geki")
    hit300 = json.optInt("perfect")
    hit100k = json.optInt("katu")
    hit100 = json.optInt("good")
    hit50 = json.optInt("bad")
    misses = json.optInt("miss")
    notes = hit300 + hit100 + hit50 + misses
    accuracy = (hit300 * 6f + hit100 * 2f + hit50) / ((hit300 + hit100 + hit50 + misses) * 6f)
    isAlive = json.getBoolean("isAlive")
}
