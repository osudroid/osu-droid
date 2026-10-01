package com.osudroid.online.model

import com.osudroid.data.ScoreInfo

data class BeatmapScoreModel(
    val scoreId: Long,
    val rank: Int,
    val mark: String = "",
    val playerName: String,
    /**
     * The serialized mods of the score, see [com.osudroid.utils.deserializeMods].
     */
    val mods: String,
    val score: Long,
    val pp: Float,
    val maxCombo: Int,
    val accuracy: Float,
    val avatarUrl: String? = null,
    /**
     * Whether this score is the personal best of the current user.
     */
    val isPersonalBest: Boolean = false,
)

fun BeatmapScore(rank: Int, info: ScoreInfo): BeatmapScoreModel {
    return BeatmapScoreModel(
        scoreId = info.id,
        rank = rank,
        mark = info.mark,
        playerName = info.playerName,
        mods = info.mods,
        score = info.score,
        pp = 0f,
        maxCombo = info.maxCombo,
        accuracy = info.accuracy
    )
}
