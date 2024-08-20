package com.reco1l.osu

import androidx.room.*
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import kotlin.math.max
import kotlin.math.min
import com.rian.osu.beatmap.Beatmap as RianBeatmap


/// Ported from rimu! project

@Entity(indices = [
    Index(name = "parentPathIdx", value = ["parentPath"]),
    Index(name = "parentIdx", value = ["parentPath", "parentId"])
])
data class BeatmapInfo(

    /**
     * The beatmap path.
     */
    @PrimaryKey
    val path: String,

    /**
     * The beatmap MD5.
     */
    @get:JvmName("getMD5")
    val md5: String,

    /**
     * The beatmap ID.
     */
    val id: Long?,

    /**
     * The audio filename.
     */
    val audio: String,

    /**
     * The background filename.
     */
    val background: String?,


    // Online

    /**
     * The beatmap ranked status.
     */
    val status: Int?,


    // Parent set

    /**
     * This indicates the parent set path.
     */
    val parentPath: String,

    /**
     * This indicates the parent set ID.
     */
    val parentId: Int?,


    // Metadata

    /**
     * The title.
     */
    val title: String,

    /**
     * The title in unicode.
     */
    val titleUnicode: String,

    /**
     * The artist.
     */
    val artist: String,

    /**
     * The artist in unicode.
     */
    val artistUnicode: String,

    /**
     * The beatmap creator.
     */
    val creator: String,

    /**
     * The beatmap version.
     */
    val version: String,

    /**
     * The beatmap tags.
     */
    val tags: String,

    /**
     * The beatmap source.
     */
    val source: String,

    /**
     * The date when the beatmap has been imported
     */
    val dateImported: Long,


    // Cached difficulty

    /**
     * The approach rate.
     */
    val approachRate: Float,

    /**
     * The overall difficulty.
     */
    val overallDifficulty: Float,

    /**
     * The circle size.
     */
    val circleSize: Float,

    /**
     * The HP drain rate
     */
    val hpDrainRate: Float,

    /**
     * The cached osu!droid star rating.
     */
    var droidStarRating: Float?,

    /**
     * The cached osu!std star rating.
     */
    var standardStarRating: Float?,

    /**
     * The max BPM.
     */
    val bpmMax: Float,

    /**
     * The min BPM.
     */
    val bpmMin: Float,

    /**
     * The total length of the beatmap.
     */
    val length: Long,

    /**
     * The preview time.
     */
    val previewTime: Int,

    /**
     * The hit circle count.
     */
    var hitCircleCount: Int,

    /**
     * The slider count.
     */
    var spinnerCount: Int,

    /**
     * The spinner count.
     */
    var sliderCount: Int,

    /**
     * The max combo.
     */
    var maxCombo: Int

) {

    /**
     * The total hit object count.
     */
    val totalHitObjectCount
        get() = hitCircleCount + sliderCount + spinnerCount

    /**
     * Returns the title text based on the current configuration, whether romanization is forced it
     * will return [title], otherwise [titleUnicode] if it's not blank.
     */
    val titleText
        get() = if (Config.isForceRomanized()) title else titleUnicode.takeUnless { it.isBlank() } ?: title


    /**
     * Returns the artist text based on the current configuration, whether romanization is forced it
     * will return [artist], otherwise [artistUnicode] if it's not blank.
     */
    val artistText
        get() = if (Config.isForceRomanized()) artist else artistUnicode.takeUnless { it.isBlank() } ?: artist


    /**
     * Returns the star rating based on the current algorithm configuration, whether droid or standard.
     * Additionally you can pass a custom algorithm to get the star rating.
     */
    @JvmOverloads
    fun getStarRating(algorithm: DifficultyAlgorithm = Config.getDifficultyAlgorithm()) = when (algorithm) {
        DifficultyAlgorithm.standard -> standardStarRating ?: 0f
        DifficultyAlgorithm.droid -> droidStarRating ?: 0f
    }


    override fun equals(other: Any?) = other is BeatmapInfo && other.path == path


    companion object {

        /**
         * Parse a new [BeatmapInfo] from a [RianBeatmap] instance.
         */
        @JvmStatic
        fun from(data: RianBeatmap, parentPath: String, lastModified: Long, path: String, withDifficulty: Boolean): BeatmapInfo {

            var bpmMin = Float.MAX_VALUE
            var bpmMax = 0f

            // Timing points
            data.controlPoints.timing.getControlPoints().fastForEach {

                val bpm = it.bpm.toFloat()

                bpmMin = if (bpmMin != Float.MAX_VALUE) min(bpmMin, bpm) else bpm
                bpmMax = if (bpmMax != 0f) max(bpmMax, bpm) else bpm
            }

            data.hitObjects.objects.isEmpty()

            var droidStarRating: Float? = null
            var standardStarRating: Float? = null

            if (withDifficulty) {
                val droidAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(data)
                val standardAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(data)

                droidStarRating = GameHelper.Round(droidAttributes.starRating, 2)
                standardStarRating = GameHelper.Round(standardAttributes.starRating, 2)
            }

            return BeatmapInfo(

                md5 = data.md5,
                id = data.metadata.beatmapId.toLong(),
                path = path,
                audio = data.folder!! + '/' + data.general.audioFilename,
                background = data.folder!! + '/' + data.events.backgroundFilename,

                // Online
                status = null, // TODO: Should we cache ranking status ?

                // Parent set
                parentPath = parentPath,
                parentId = data.metadata.beatmapSetId,

                // Metadata
                title = data.metadata.title,
                titleUnicode = data.metadata.titleUnicode,
                artist = data.metadata.artist,
                artistUnicode = data.metadata.artistUnicode,
                creator = data.metadata.creator,
                version = data.metadata.version,
                tags = data.metadata.tags,
                source = data.metadata.source,
                dateImported = lastModified,

                // Difficulty
                approachRate = data.difficulty.ar,
                overallDifficulty = data.difficulty.od,
                circleSize = data.difficulty.cs,
                hpDrainRate = data.difficulty.hp,
                droidStarRating = droidStarRating,
                standardStarRating = standardStarRating,
                bpmMin = bpmMin,
                bpmMax = bpmMax,
                length = data.duration.toLong(),
                previewTime = data.general.previewTime,
                hitCircleCount = if (withDifficulty) data.hitObjects.circleCount else 0,
                sliderCount = if (withDifficulty) data.hitObjects.sliderCount else 0,
                spinnerCount = if (withDifficulty) data.hitObjects.spinnerCount else 0,
                maxCombo = if (withDifficulty) data.maxCombo else 0
            )
        }
    }
}


@Dao
interface IBeatmapDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(beatmapInfo: BeatmapInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(beatmapInfo: List<BeatmapInfo>)

    @Query("DELETE FROM BeatmapInfo WHERE parentPath = :beatmapSetKey")
    fun deleteBeatmapSet(beatmapSetKey: String)

    @Transaction
    @Query("SELECT DISTINCT parentPath, parentId FROM BeatmapInfo")
    fun getBeatmapSetList() : List<BeatmapSetInfo>

    @Transaction
    @Query("SELECT DISTINCT parentPath FROM BeatmapInfo")
    fun getBeatmapSetPaths() : List<String>

    @Query("DELETE FROM BeatmapInfo")
    fun deleteAll()
}



/**
 * Defines a beatmap set, they're virtually created by the database using DISTINCT operation. This means it doesn't have
 * a table.
 */
data class BeatmapSetInfo(

    /**
     * The ID.
     */
    @ColumnInfo(name = "parentId")
    val id: Int?,

    /**
     * This can equal to the set ID or its MD5.
     */
    @ColumnInfo(name = "parentPath")
    val path: String,

    /**
     * The list of beatmaps
     */
    @Relation(parentColumn = "parentPath", entityColumn = "parentPath")
    val beatmaps: List<BeatmapInfo>

) {

    /**
     * The beatmap set size.
     */
    val count
        get() = beatmaps.size


    /**
     * Get a beatmap from its index.
     */
    operator fun get(index: Int) = beatmaps[index]


    override fun equals(other: Any?) = other is BeatmapSetInfo && other.path == path

}
