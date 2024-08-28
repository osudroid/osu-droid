@file:JvmName("Scores")
package com.reco1l.osu.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


@Entity(
    indices = [
        Index(name = "beatmapPathIdx", value = ["beatmapPath"])
    ]
)
data class ScoreInfo @JvmOverloads constructor(

    /**
     * The score ID.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * The beatmap filename.
     */
    val beatmapFilename: String,

    /**
     * The beatmap set directory.
     */
    val beatmapSetDirectory: String,

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

    /**
     * The replay file path.
     */
    val replayPath
        get() = "${Config.getScorePath()}/$replayFilename"


    fun toJSON() = JSONObject().apply {

        // The keys don't correspond to the table columns in order to keep compatibility with the old replays.
        put("id", id)
        put("filename", "$beatmapSetDirectory/$beatmapFilename")
        put("playername", playerName)
        put("replayfile", replayFilename)
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

    fun toStatisticV2() = StatisticV2().also {

        it.playerName = playerName
        it.fileName = "$beatmapSetDirectory/$beatmapFilename"
        it.replayFilename = replayFilename
        it.setModFromString(mods)
        it.setForcedScore(score)
        it.maxCombo = maxCombo
        it.mark = mark
        it.hit300k = hit300k
        it.hit300 = hit300
        it.hit100k = hit100k
        it.hit100 = hit100
        it.hit50 = hit50
        it.misses = misses
        it.accuracy = accuracy
        it.time = time
        it.isPerfect = isPerfect

    }


}

fun ScoreInfo(json: JSONObject) = ScoreInfo(

    // "filename" can contain the full path, so we need to extract both filename and directory name
    // which refers to the beatmap set directory. The pattern could be `/beatmapSetDirectory/beatmapFilename/`
    // with or without the trailing slash.
    beatmapFilename = json.getString("filename").let {
        if (it.endsWith('/')) {
            it.substring(0, it.length - 1).substringAfterLast('/')
        } else {
            it.substringAfterLast('/')
        }
    },
    beatmapSetDirectory = json.getString("filename").let {
        if (it.endsWith('/')) {
            it.substringBeforeLast('/').substringBeforeLast('/').substringAfterLast('/')
        } else {
            it.substringBeforeLast('/').substringAfterLast('/')
        }
    },
    replayFilename = json.getString("replayfile").let {
        if (it.endsWith('/')) {
            it.substring(0, it.length - 1).substringAfterLast('/')
        } else {
            it.substringAfterLast('/')
        }
    },

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
    accuracy = json.getDouble("accuracy").toFloat(),
    time = json.getLong("time"),
    isPerfect = json.getInt("isPerfect") == 1
)


@Dao
interface IScoreInfoDAO {

    @Query("SELECT * FROM ScoreInfo WHERE beatmapSetDirectory = :beatmapSetDirectory AND beatmapFilename = :beatmapFilename")
    fun getBeatmapScores(beatmapSetDirectory: String, beatmapFilename: String): List<ScoreInfo>

    @Query("SELECT * FROM ScoreInfo WHERE id = :id")
    fun getScore(id: Int): ScoreInfo?

    @Query("SELECT mark FROM ScoreInfo WHERE beatmapSetDirectory = :beatmapSetDirectory AND beatmapFilename = :beatmapFilename ORDER BY score DESC LIMIT 1")
    fun getBestMark(beatmapSetDirectory: String, beatmapFilename: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertScore(score: ScoreInfo): Long

    @Query("DELETE FROM ScoreInfo WHERE id = :id")
    fun deleteScore(id: Int): Int

}