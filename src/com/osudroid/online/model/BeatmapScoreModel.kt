package com.osudroid.online.model

import com.osudroid.data.*
import org.json.*

data class BeatmapScoreModel(
    val scoreId: Long,
    val rank: Int,
    val mark: String = "",
    val playerName: String,
    val mods: JSONArray,
    val score: Int,
    val pp: Float,
    val maxCombo: Int,
    val accuracy: Float,
    val avatarUrl: String? = null,
)

fun BeatmapScore(json: JSONObject): BeatmapScoreModel {
    return BeatmapScoreModel(
        scoreId = json.getLong("Id"),
        rank = -1,
        mark = json.optString("Mark", ""),
        playerName = json.getString("Username"),
        mods = json.getJSONArray("Mods"),
        score = json.getInt("Score"),
        pp = json.getDouble("Pp").toFloat(),
        maxCombo = json.getInt("Combo"),
        accuracy = json.getDouble("Accuracy").toFloat(),
        avatarUrl = json.optString("Avatar")
    )
}

fun BeatmapScore(info: ScoreInfo): BeatmapScoreModel {
    return BeatmapScoreModel(
        scoreId = info.id,
        rank = 0,
        mark = info.mark,
        playerName = info.playerName,
        mods = JSONArray(info.mods),
        score = info.score,
        pp = 0f,
        maxCombo = info.maxCombo,
        accuracy = info.accuracy
    )
}
