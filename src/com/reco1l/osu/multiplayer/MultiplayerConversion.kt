@file:JvmName("MultiplayerConverter")

package com.reco1l.osu.multiplayer

import com.rian.osu.mods.LegacyModConverter
import com.rian.osu.utils.ModUtils
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


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
    mod = LegacyModConverter.convertModString(json.optString("modstring"))
    scoreMaxCombo = json.optInt("maxCombo")
    hit300k = json.optInt("geki")
    hit300 = json.optInt("perfect")
    hit100k = json.optInt("katu")
    hit100 = json.optInt("good")
    hit50 = json.optInt("bad")
    misses = json.optInt("miss")
    isAlive = json.getBoolean("isAlive")
}
