package com.osudroid.online

import android.util.Log
import com.osudroid.online.model.BeatmapScoreModel
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Interface for Bancho API requests.
 *
 * This interface is intended to replace the original iBancho API that is spread
 * across [OnlineManager] and [ru.nsu.ccfit.zuev.osu.online.OnlineScoring] classes.
 */
object IBanchoAPI {

    /**
     * Fetches the global leaderboard of the beatmap with the given [hash].
     *
     * The production `getrank.php` endpoint answers in the legacy line format (one score per line,
     * whitespace separated), so the request goes through [OnlineManager.getTop] and the lines are
     * parsed the same way the legacy scoreboard does. The scoring type (score/pp) follows the
     * leaderboard scoring mode setting.
     *
     * Lines with 9 fields are leaderboard entries, a line with 10 fields is the personal best of the
     * current user that did not make it into the leaderboard (the 10th field is its rank). Such an
     * entry is returned with [BeatmapScoreModel.isInLeaderboard] set to `false`.
     */
    @Throws(OnlineManager.OnlineManagerException::class)
    fun getBeatmapLeaderboard(hash: String): List<BeatmapScoreModel> {
        val lines = OnlineManager.getInstance().getTop(hash)
        val username = OnlineManager.getInstance().username
        val list = ArrayList<BeatmapScoreModel>(lines.size)

        for ((index, line) in lines.withIndex()) {
            val data = line.trim().split(Regex("\\s+"))

            if (data.size < 9 || data.size > 10) {
                continue
            }

            try {
                val isInLeaderboard = data.size == 9
                val isPersonalBest = data.size == 10 || data[1] == username

                list.add(
                    BeatmapScoreModel(
                        scoreId = data[0].toLong(),
                        rank = if (isInLeaderboard) index + 1 else data[9].toInt(),
                        mark = data[5],
                        playerName = if (isPersonalBest) username else data[1],
                        mods = data[6],
                        score = data[2].toLong(),
                        pp = data[3].toFloat(),
                        maxCombo = data[4].toInt(),
                        accuracy = data[7].toFloat(),
                        avatarUrl = data[8].takeIf { it.isNotEmpty() },
                        isPersonalBest = isPersonalBest,
                        isInLeaderboard = isInLeaderboard,
                    )
                )
            } catch (e: NumberFormatException) {
                Log.w("IBanchoAPI", "Skipping malformed leaderboard line: $line", e)
            }
        }

        return list
    }

}
