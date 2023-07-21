@file:JvmName("MultiplayerConverter")

package com.reco1l.legacy.data

import com.reco1l.api.ibancho.data.WinCondition
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoard.ScoreBoardItems
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.util.*

fun modsToReadable(mods: String?): String
{
    if (mods.isNullOrEmpty()) return "None"

    val b = StringBuilder()

    if ('a' in mods) b.append("Auto,")
    if ('x' in mods) b.append("Relax,")
    if ('p' in mods) b.append("AP,")
    if ('e' in mods) b.append("EZ,")
    if ('n' in mods) b.append("NF,")
    if ('r' in mods) b.append("HR,")
    if ('h' in mods) b.append("HD,")
    if ('i' in mods) b.append("FL,")
    if ('d' in mods) b.append("DT,")
    if ('c' in mods) b.append("NC,")
    if ('t' in mods) b.append("HT,")
    if ('s' in mods) b.append("PR,")
    if ('l' in mods) b.append("REZ,")
    if ('m' in mods) b.append("SC,")
    if ('u' in mods) b.append("SD,")
    if ('f' in mods) b.append("PF,")
    if ('b' in mods) b.append("SU,")
    if ('v' in mods) b.append("ScoreV2,")

    return b.substring(0, b.length - 1)
}

fun stringToMods(data: String?): EnumSet<GameMod>
{
    val mod = EnumSet.noneOf(GameMod::class.java)

    if (data.isNullOrEmpty()) return mod

    if ('a' in data) mod += MOD_AUTO
    if ('x' in data) mod += MOD_RELAX
    if ('p' in data) mod += MOD_AUTOPILOT
    if ('e' in data) mod += MOD_EASY
    if ('n' in data) mod += MOD_NOFAIL
    if ('r' in data) mod += MOD_HARDROCK
    if ('h' in data) mod += MOD_HIDDEN
    if ('i' in data) mod += MOD_FLASHLIGHT
    if ('d' in data) mod += MOD_DOUBLETIME
    if ('c' in data) mod += MOD_NIGHTCORE
    if ('t' in data) mod += MOD_HALFTIME
    if ('s' in data) mod += MOD_PRECISE
    if ('m' in data) mod += MOD_SMALLCIRCLE
    if ('l' in data) mod += MOD_REALLYEASY
    if ('u' in data) mod += MOD_SUDDENDEATH
    if ('f' in data) mod += MOD_PERFECT
    if ('v' in data) mod += MOD_SCOREV2

    return mod
}

fun modsToString(mod: EnumSet<GameMod>): String
{
    val s = StringBuilder()

    if (MOD_AUTO in mod) s.append('a')
    if (MOD_RELAX in mod) s.append('x')
    if (MOD_AUTOPILOT in mod) s.append('p')
    if (MOD_EASY in mod) s.append('e')
    if (MOD_NOFAIL in mod) s.append('n')
    if (MOD_HARDROCK in mod) s.append('r')
    if (MOD_HIDDEN in mod) s.append('h')
    if (MOD_FLASHLIGHT in mod) s.append('i')
    if (MOD_DOUBLETIME in mod) s.append('d')
    if (MOD_NIGHTCORE in mod) s.append('c')
    if (MOD_HALFTIME in mod) s.append('t')
    if (MOD_PRECISE in mod) s.append('s')
    if (MOD_SMALLCIRCLE in mod) s.append('m')
    if (MOD_REALLYEASY in mod) s.append('l')
    if (MOD_PERFECT in mod) s.append('f')
    if (MOD_SUDDENDEATH in mod) s.append('u')
    if (MOD_SCOREV2 in mod) s.append('v')

    return s.toString()
}

fun jsonToScoreboardItem(json: JSONObject) = ScoreBoardItems().apply {

    userName = json.getString("username")
    playScore = json.getInt("score")

    // Provided by event 'liveScoreData'
    maxCombo = json.optInt("combo")
    accuracy = json.getDouble("accuracy").toFloat()

}

fun jsonToStatistic(json: JSONObject) = StatisticV2().apply {

    playerName = json.getString("username")
    totalScore = json.getInt("score")
    time = System.currentTimeMillis()

    // Provided by event 'liveScoreData'
    combo = json.optInt("combo")

    // Provided by event 'scoreSubmission'
    mod = stringToMods(if (json.isNull("mods")) null else json.getString("mods"))
    maxCombo = json.optInt("maxCombo")
    hit300k = json.optInt("geki")
    hit300 = json.optInt("perfect")
    hit100k = json.optInt("katu")
    hit100 = json.optInt("good")
    hit50 = json.optInt("bad")
    misses = json.optInt("miss")
    notes = hit300 + hit100 + hit50 + misses

    // Special case, only 'liveScoreData' provides it but 'scoreSubmission' doesn't and must be calculated in client.
    accuracy = if (json.has("accuracy"))
        json.getDouble("accuracy").toFloat()
    else
        (hit300 * 6f + hit100 * 2f + hit50) / ((hit300 + hit100 + hit50 + misses) * 6f)

}

fun statisticToJson(stats: StatisticV2, isLiveScore: Boolean) = JSONObject().apply {

    put("accuracy", stats.accuracyForServer)

    if (isLiveScore)
    {
        put("score", stats.modifiedTotalScore)
        put("combo", if (RoomScene.room!!.winCondition == WinCondition.MAX_COMBO) stats.maxCombo else stats.combo)
        return@apply
    }
    else put("score", stats.totalScore)

    put("username", stats.playerName)
    put("modstring", modsToString(stats.mod))
    put("maxCombo", stats.maxCombo)
    put("geki", stats.hit300k)
    put("perfect", stats.hit300)
    put("katu", stats.hit100k)
    put("good", stats.hit100)
    put("bad", stats.hit50)
    put("miss", stats.misses)

}