package com.reco1l.osu.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


@Entity
data class Score @JvmOverloads constructor(

    /**
     * The score ID.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The beatmap file name.
     */
    val beatmapFilename: String,

    /**
     * The player name.
     */
    val playerName: String,

    /**
     * The replay file path.
     */
    var replayPath: String,

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
     * The accuracy.
     */
    val accuracy: Float,

    /**
     * The score date.
     */
    val time: Long,

    /**
     * Whether the score is perfect.
     */
    val isPerfect: Boolean

) {


    fun toJSON() = JSONObject().apply {

        // The keys doesn't correspond to the table columns in order to keep compatibility with the
        // old replays.
        put("id", id)
        put("filename", beatmapFilename)
        put("playername", playerName)
        put("replayfile", replayPath)
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
        put("isPerfect", if (isPerfect) 1 else 0)

    }


    companion object {

        @JvmStatic
        fun fromStatisticV2(stats: StatisticV2, beatmapFile: String, replayPath: String) = Score(
            beatmapFilename = beatmapFile,
            playerName = stats.playerName,
            replayPath = replayPath,
            mods = stats.modString,
            score = stats.totalScoreWithMultiplier,
            maxCombo = stats.combo,
            mark = stats.mark,
            hit300k = stats.hit300k,
            hit300 = stats.hit300,
            hit100k = stats.hit100k,
            hit100 = stats.hit100,
            hit50 = stats.hit50,
            misses = stats.misses,
            accuracy = stats.accuracy,
            time = stats.time,
            isPerfect = stats.isPerfect
        )


        @JvmStatic
        fun fromJSON(json: JSONObject) = Score(
            // The keys doesn't correspond to the table columns in order to keep compatibility with the
            // old replays.
            id = json.optLong("id", 0),
            beatmapFilename = json.getString("filename"),
            playerName = json.getString("playername"),
            replayPath = json.getString("replayfile"),
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
            accuracy = json.getDouble("accuracy").toFloat(),
            time = json.getLong("time"),
            isPerfect = json.getInt("isPerfect") == 1
        )

    }

}


@Dao
interface IScoreDAO {

    @Query("SELECT * FROM Score WHERE beatmapFilename = :beatmapFilename")
    fun getBeatmapScores(beatmapFilename: String): List<Score>

    @Query("SELECT * FROM Score WHERE id = :id")
    fun getScore(id: Int): Score?

    @Query("SELECT mark FROM Score WHERE beatmapFilename = :beatmapFilename ORDER BY score DESC LIMIT 1")
    fun getBestMark(beatmapFilename: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertScore(score: Score): Long

    @Query("DELETE FROM Score WHERE id = :id")
    fun deleteScore(id: Int): Int

    @Query("SELECT id FROM Score WHERE beatmapFilename = :beatmapFilename ORDER BY score DESC LIMIT 1")
    fun getBestScoreId(beatmapFilename: String): Int?

}