package com.reco1l.osu

import androidx.room.*
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import kotlin.math.max
import kotlin.math.min
import com.rian.osu.beatmap.Beatmap as RianBeatmap


/// Ported from rimu! project

@Entity
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
    val parentId: Int,


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
    val droidStarRating: Float,

    /**
     * The cached osu!std star rating.
     */
    val standardStarRating: Float,

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
     * The total hit object count.
     */
    val totalHitObjectCount: Int,

    /**
     * The hit circle count.
     */
    val hitCircleCount: Int,

    /**
     * The slider count.
     */
    val spinnerCount: Int,

    /**
     * The spinner count.
     */
    val sliderCount: Int,

    /**
     * The max combo.
     */
    val maxCombo: Int

) {


    override fun equals(other: Any?) = other is BeatmapInfo && other.path == path


    companion object {

        /**
         * Parse a new [BeatmapInfo] from a [RianBeatmap] instance.
         */
        @JvmStatic
        fun from(data: RianBeatmap, parentPath: String, lastModified: Long, path: String): BeatmapInfo {

            var bpmMin = 0f
            var bpmMax = 0f

            // Timing points
            for (point in data.controlPoints.timing.getControlPoints()) {

                val bpm = point.bpm.toFloat()

                bpmMin = if (bpmMin != Float.MAX_VALUE) min(bpmMin, bpm) else bpm
                bpmMax = if (bpmMax != 0f) max(bpmMax, bpm) else bpm
            }

            val droidAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(data)
            val standardAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(data)

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
                droidStarRating = GameHelper.Round(droidAttributes.starRating, 2),
                standardStarRating = GameHelper.Round(standardAttributes.starRating, 2),
                bpmMin = bpmMin,
                bpmMax = bpmMax,
                length = data.duration.toLong(),
                previewTime = data.general.previewTime,
                totalHitObjectCount = data.hitObjects.objects.size,
                hitCircleCount = data.hitObjects.circleCount,
                sliderCount = data.hitObjects.sliderCount,
                spinnerCount = data.hitObjects.spinnerCount,
                maxCombo = data.maxCombo
            )
        }
    }
}


@Dao
interface IBeatmapDAO {

    /**
     * Insert a new beatmap to the table.
     *
     * [OnConflictStrategy.REPLACE]
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(beatmapInfo: BeatmapInfo): Long

    /**
     * Delete a beatmap set from the table using it key (MD5 or ID).
     */
    @Query("DELETE FROM BeatmapInfo WHERE parentPath = :beatmapSetKey")
    fun deleteBeatmapSet(beatmapSetKey: String)

    /**
     * Get a flow that listens to database changes for the beatmap table.
     * It collects a list of [BeatmapSetInfo] that each one wraps its corresponding [BeatmapInfo].
     */
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
    val id: Int,

    /**
     * This can equal to the set ID or its MD5.
     */
    @ColumnInfo(name = "parentPath")
    val path: String,

    /**The list of beatmaps*/
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
