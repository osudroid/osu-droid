@file:JvmName("Scores")

package com.osudroid.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.IModRequiresBeatmapDifficulty
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.utils.ModUtils
import kotlin.math.roundToInt
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
    var mods: String,

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

    /**
     * The amount of slider heads that were hit.
     */
    var sliderHeadHits: Int?,

    /**
     * The amount of slider ticks that were hit.
     */
    var sliderTickHits: Int?,

    /**
     * The amount of slider repeats that were hit.
     */
    var sliderRepeatHits: Int?,

    /**
     * The amount of slider ends that were hit.
     */
    var sliderEndHits: Int?,

    /**
     * Whether this score's [score] still holds [StatisticV2.totalScoreWithMultiplier] and needs on-the-fly conversion
     * to raw [StatisticV2.totalScore] once the beatmap is available.
     *
     * This flag is set during database migration when a score uses a mod that requires beatmap data (e.g.
     * [ModDifficultyAdjust]) and the beatmap was not present in the library at migration time. Call
     * [IScoreInfoDAO.migrateScores] whenever the beatmap becomes available to complete the conversion.
     */
    var needsScoreMigration: Boolean = false
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

    /**
     * Calculates the score of this [ScoreInfo] after applying score multipliers from [mods].
     *
     * Pass [difficulty] when available so that mods implementing [IModRequiresBeatmapDifficulty] (e.g.
     * [ModDifficultyAdjust]) can apply their beatmap-dependent multiplier. Without it, their score multiplier is 1.
     *
     * Use [StatisticV2.getTotalScoreWithMultiplier] with a proper [StatisticV2.calculateModScoreMultiplier] call for
     * display when the full [BeatmapDifficulty] is available.
     */
    @JvmOverloads
    fun calculateEffectiveScore(difficulty: BeatmapDifficulty? = null): Int {
        // Pending-migration rows store total score with multipliers directly, so multiplying again would apply the
        // multiplier twice.
        if (needsScoreMigration) {
            return score
        }

        val modMap = ModUtils.deserializeMods(mods)

        if (difficulty != null) {
            modMap.values.filterIsInstance<IModRequiresBeatmapDifficulty>().forEach { m ->
                m.applyFromBeatmapDifficulty(difficulty)
            }
        }

        return (score * ModUtils.calculateScoreMultiplier(modMap)).roundToInt()
    }


    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun toStatisticV2(difficulty: BeatmapDifficulty? = null) = StatisticV2().also {

        it.playerName = playerName
        it.setBeatmapMD5(beatmapMD5)
        it.replayFilename = replayFilename
        it.mod = ModUtils.deserializeMods(mods)

        // Pending-migration rows store total score with multiplier, so we use forced score to prevent mod multipliers
        // from multiplying again.
        if (needsScoreMigration) {
            it.setForcedScore(score)
        } else {
            it.totalScore = score
        }

        it.scoreMaxCombo = maxCombo
        it.mark = mark
        it.hit300k = hit300k
        it.hit300 = hit300
        it.hit100k = hit100k
        it.hit100 = hit100
        it.hit50 = hit50
        it.misses = misses
        it.time = time
        it.sliderHeadHits = sliderHeadHits ?: -1
        it.sliderTickHits = sliderTickHits ?: -1
        it.sliderRepeatHits = sliderRepeatHits ?: -1
        it.sliderEndHits = sliderEndHits ?: -1

        if (difficulty != null) {
            it.migrateLegacyMods(difficulty)
        }

        it.calculateModScoreMultiplier(difficulty)
    }


}

fun ScoreInfo(json: JSONObject) =
    ScoreInfo(

        beatmapMD5 = json.getString("beatmapMD5"),
        replayFilename = FilenameUtils.getName(json.getString("replayfile")),

        // The keys don't correspond to the table columns in order to keep compatibility with the old replays.
        id = json.optLong("id", 0),
        playerName = json.getString("playername"),
        mods = json.getString("mods"),
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
        sliderHeadHits = json.optInt("sliderHeadHits", -1).takeIf { it >= 0 },
        sliderTickHits = json.optInt("sliderTickHits", -1).takeIf { it >= 0 },
        sliderRepeatHits = json.optInt("sliderRepeatHits", -1).takeIf { it >= 0 },
        sliderEndHits = json.optInt("sliderEndHits", -1).takeIf { it >= 0 }
    )

/**
 * A [ScoreInfo] paired with its precomputed effective score, as returned by [IScoreInfoDAO.getBeatmapLeaderboard].
 */
data class ScoredScoreInfo(val scoreInfo: ScoreInfo, val effectiveScore: Int)

@Dao
interface IScoreInfoDAO {

    @Query("SELECT * FROM ScoreInfo WHERE beatmapMD5 = :beatmapMD5")
    fun getBeatmapScores(beatmapMD5: String): List<ScoreInfo>

    fun getBeatmapLeaderboard(beatmapMD5: String, difficulty: BeatmapDifficulty? = null) =
        getBeatmapScores(beatmapMD5)
            .map { ScoredScoreInfo(it, it.calculateEffectiveScore(difficulty)) }
            .sortedByDescending { it.effectiveScore }

    @Query("SELECT * FROM ScoreInfo WHERE id = :id")
    fun getScore(id: Int): ScoreInfo?

    fun getBestMark(beatmapMD5: String, difficulty: BeatmapDifficulty? = null) =
        getBeatmapScores(beatmapMD5).maxByOrNull { it.calculateEffectiveScore(difficulty) }?.mark

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertScore(score: ScoreInfo): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertScores(scores: List<ScoreInfo>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateScore(score: ScoreInfo)

    @Query("DELETE FROM ScoreInfo WHERE id = :id")
    fun deleteScore(id: Int): Int

    @Query("SELECT EXISTS(SELECT 1 FROM ScoreInfo WHERE id = :id)")
    fun scoreExists(id: Long): Boolean

    @Query("SELECT * FROM ScoreInfo WHERE beatmapMD5 = :beatmapMD5 AND needsScoreMigration = 1")
    fun getScoresNeedingMigration(beatmapMD5: String): List<ScoreInfo>

    /**
     * Completes pending score migrations for a beatmap.
     *
     * Scores flagged with [ScoreInfo.needsScoreMigration] still hold the old [StatisticV2.totalScoreWithMultiplier]
     * value because their beatmap was absent during the database migration. Now that [difficulty] is available, the
     * correct mod multiplier can be computed and the raw [StatisticV2.totalScore] stored.
     *
     * Call this whenever the beatmap becomes available before displaying scores.
     */
    fun migrateScores(beatmapMD5: String, difficulty: BeatmapDifficulty) {
        val pending = getScoresNeedingMigration(beatmapMD5)

        if (pending.isEmpty()) {
            return
        }

        for (scoreInfo in pending) {
            val mods = ModUtils.deserializeMods(scoreInfo.mods)

            mods.values.filterIsInstance<IModRequiresBeatmapDifficulty>().forEach {
                it.applyFromBeatmapDifficulty(difficulty)
            }

            updateScore(scoreInfo.copy(
                score = (scoreInfo.score / ModUtils.calculateMigrationScoreMultiplier(mods)).roundToInt(),
                mods = mods.serializeMods(),
                needsScoreMigration = false
            ))
        }
    }
}