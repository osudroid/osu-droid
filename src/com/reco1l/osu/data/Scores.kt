@file:JvmName("Scores")
package com.reco1l.osu.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import org.apache.commons.io.FilenameUtils
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


@Entity(
    indices = [
        Index(name = "beatmapIdx", value = ["beatmapMD5"]),
    ]
)
data class ScoreInfo @JvmOverloads constructor(

    /**
     * The score ID.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The MD5 hash of the beatmap.
     */
    val beatmapMD5: String,

    /**
     * The player name.
     */
    val playerName: String,

    /**
     * The replay file path.
     */
    var replayFilename: String,

    /**
     * The mods used.
     */
    val mods: String,

    /**
     * The total score.
     */
    val score: Int,

    /**
     * The maximum combo.
     */
    val maxCombo: Int,

    /**
     * The mark.
     */
    val mark: String,

    /**
     * The number of 300k hits.
     */
    val hit300k: Int,

    /**
     * The number of 300 hits.
     */
    val hit300: Int,

    /**
     * The number of 100k hits.
     */
    val hit100k: Int,

    /**
     * The number of 100 hits.
     */
    val hit100: Int,

    /**
     * The number of 50 hits.
     */
    val hit50: Int,

    /**
     * The number of misses.
     */
    val misses: Int,

    /**
     * The score date.
     */
    val time: Long,

) {

    /**
     * The replay file path.
     */
    val replayPath
        get() = "${Config.getScorePath()}/$replayFilename"

    /**
     * The number of notes hit.
     */
    val notesHit
        get() = hit300 + hit100 + hit50 + misses

    /**
     * The accuracy.
     */
    val accuracy
        get() = if (notesHit == 0) 1f else (hit300 * 6f + hit100 * 2f + hit50) / (6f * notesHit)


    fun toJSON() = JSONObject().apply {

        // The keys don't correspond to the table columns in order to keep compatibility with the old replays.
        put("id", id)
        put("playername", playerName)
        put("replayfile", replayFilename)
        put("beatmapMD5", beatmapMD5)
        put("mod", mods)
        put("score", score)
        put("combo", maxCombo)
        put("mark", mark)
        put("h300k", hit300k)
        put("h300", hit300)
        put("h100k", hit100k)
        put("h100", hit100)
        put("h50", hit50)
        put("misses", misses)
        put("accuracy", accuracy)
        put("time", time)

    }

    fun toStatisticV2() = StatisticV2().also {

        it.playerName = playerName
        it.setBeatmapMD5(beatmapMD5)
        it.replayFilename = replayFilename
        it.setModFromString(mods)
        it.setForcedScore(score)
        it.scoreMaxCombo = maxCombo
        it.mark = mark
        it.hit300k = hit300k
        it.hit300 = hit300
        it.hit100k = hit100k
        it.hit100 = hit100
        it.hit50 = hit50
        it.misses = misses
        it.time = time

    }


}

fun ScoreInfo(json: JSONObject) =
    ScoreInfo(

        beatmapMD5 = json.getString("beatmapMD5"),
        replayFilename = FilenameUtils.getName(json.getString("replayfile")),

        // The keys don't correspond to the table columns in order to keep compatibility with the old replays.
        id = json.optLong("id", 0),
        playerName = json.getString("playername"),
        mods = json.getString("mod"),
        score = json.getInt("score"),
        maxCombo = json.getInt("combo"),
        mark = json.getString("mark"),
        hit300k = json.getInt("h300k"),
        hit300 = json.getInt("h300"),
        hit100k = json.getInt("h100k"),
        hit100 = json.getInt("h100"),
        hit50 = json.getInt("h50"),
        misses = json.getInt("misses"),
        time = json.getLong("time"),
    )

@Dao
interface IScoreInfoDAO {

    @Query("SELECT * FROM ScoreInfo WHERE beatmapMD5 = :beatmapMD5 ORDER BY score DESC")
    fun getBeatmapScores(beatmapMD5: String): List<ScoreInfo>

    @Query("SELECT * FROM ScoreInfo WHERE id = :id")
    fun getScore(id: Int): ScoreInfo?

    @Query("SELECT mark FROM ScoreInfo WHERE beatmapMD5 = :beatmapMD5 ORDER BY score DESC LIMIT 1")
    fun getBestMark(beatmapMD5: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertScore(score: ScoreInfo): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertScores(scores: List<ScoreInfo>)

    @Query("DELETE FROM ScoreInfo WHERE id = :id")
    fun deleteScore(id: Int): Int

    @Query("SELECT EXISTS(SELECT 1 FROM ScoreInfo WHERE id = :id)")
    fun scoreExists(id: Long): Boolean

}