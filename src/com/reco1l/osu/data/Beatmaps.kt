@file:JvmName("Beatmaps")
package com.reco1l.osu.data

import androidx.room.*
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.*
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
    var md5: String,

    /**
     * The beatmap ID.
     */
    var id: Long?,

    /**
     * The audio filename.
     */
    var audio: String,

    /**
     * The background filename.
     */
    var background: String?,


    // Online

    /**
     * The beatmap ranked status.
     */
    var status: Int?,


    // Parent set

    /**
     * This indicates the parent set path.
     */
    var parentPath: String,

    /**
     * This indicates the parent set ID.
     */
    var parentId: Int?,


    // Metadata

    /**
     * The title.
     */
    var title: String,

    /**
     * The title in unicode.
     */
    var titleUnicode: String,

    /**
     * The artist.
     */
    var artist: String,

    /**
     * The artist in unicode.
     */
    var artistUnicode: String,

    /**
     * The beatmap creator.
     */
    var creator: String,

    /**
     * The beatmap version.
     */
    var version: String,

    /**
     * The beatmap tags.
     */
    var tags: String,

    /**
     * The beatmap source.
     */
    var source: String,

    /**
     * The date when the beatmap has been imported
     */
    var dateImported: Long,


    // Cached difficulty

    /**
     * The approach rate.
     */
    var approachRate: Float,

    /**
     * The overall difficulty.
     */
    var overallDifficulty: Float,

    /**
     * The circle size.
     */
    var circleSize: Float,

    /**
     * The HP drain rate
     */
    var hpDrainRate: Float,

    /**
     * The cached osu!droid star rating.
     * Do not use this value directly, use [getStarRating] instead.
     */
    var droidStarRating: Float?,

    /**
     * The cached osu!std star rating.
     * Do not use this value directly, use [getStarRating] instead.
     */
    var standardStarRating: Float?,

    /**
     * The max BPM.
     */
    var bpmMax: Float,

    /**
     * The min BPM.
     */
    var bpmMin: Float,

    /**
     * The total length of the beatmap.
     */
    var length: Long,

    /**
     * The preview time.
     */
    var previewTime: Int,

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
     * Whether the beatmap needs a difficulty calculation.
     */
    val needsDifficultyCalculation
        get() = droidStarRating == null || standardStarRating == null


    /**
     * Returns the star rating based on the current algorithm configuration, whether droid or standard.
     * Optionally, you can pass a custom algorithm to get the star rating.
     *
     * Returns 0 if the star rating has not been calculated.
     */
    @JvmOverloads
    fun getStarRating(algorithm: DifficultyAlgorithm = Config.getDifficultyAlgorithm()) = when(algorithm) {
        droid -> droidStarRating ?: 0f
        standard -> standardStarRating ?: 0f
    }


    override fun equals(other: Any?): Boolean {
        return other is BeatmapInfo && other.path == path
    }


    fun apply(b: BeatmapInfo) {
        md5 = b.md5
        id = b.id
        audio = b.audio
        background = b.background
        status = b.status
        parentPath = b.parentPath
        parentId = b.parentId
        title = b.title
        titleUnicode = b.titleUnicode
        artist = b.artist
        artistUnicode = b.artistUnicode
        creator = b.creator
        version = b.version
        tags = b.tags
        source = b.source
        dateImported = b.dateImported
        approachRate = b.approachRate
        overallDifficulty = b.overallDifficulty
        circleSize = b.circleSize
        hpDrainRate = b.hpDrainRate
        droidStarRating = b.droidStarRating
        standardStarRating = b.standardStarRating
        bpmMax = b.bpmMax
        bpmMin = b.bpmMin
        length = b.length
        previewTime = b.previewTime
        hitCircleCount = b.hitCircleCount
        sliderCount = b.sliderCount
        spinnerCount = b.spinnerCount
        maxCombo = b.maxCombo

    }
}

fun BeatmapInfo(data: RianBeatmap, parentPath: String, lastModified: Long, path: String, calculateDifficulty: Boolean): BeatmapInfo {

    var bpmMin = Float.MAX_VALUE
    var bpmMax = 0f

    // Timing points
    data.controlPoints.timing.getControlPoints().fastForEach {

        val bpm = it.bpm.toFloat()

        bpmMin = if (bpmMin != Float.MAX_VALUE) min(bpmMin, bpm) else bpm
        bpmMax = if (bpmMax != 0f) max(bpmMax, bpm) else bpm
    }

    var droidStarRating: Float? = null
    var standardStarRating: Float? = null

    if (calculateDifficulty) {
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
        hitCircleCount = data.hitObjects.circleCount,
        sliderCount = data.hitObjects.sliderCount,
        spinnerCount = data.hitObjects.spinnerCount,
        maxCombo = data.maxCombo
    )
}

@Dao interface IBeatmapInfoDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(beatmapInfo: List<BeatmapInfo>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(beatmapInfo: BeatmapInfo)

    @Query("DELETE FROM BeatmapInfo WHERE parentPath = :path")
    fun deleteBeatmapSet(path: String)

    @Query("DELETE FROM BeatmapInfo WHERE parentPath IN (:paths)")
    fun deleteAllBeatmapSets(paths: List<String>)

    @Transaction
    @Query("SELECT DISTINCT parentPath, parentId FROM BeatmapInfo")
    fun getBeatmapSetList() : List<BeatmapSetInfo>

    @Query("SELECT DISTINCT parentPath FROM BeatmapInfo")
    fun getBeatmapSetPaths() : List<String>

    @Query("SELECT EXISTS(SELECT parentPath FROM BeatmapInfo WHERE parentPath = :path LIMIT 1)")
    fun isBeatmapSetImported(path: String): Boolean

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
    @JvmName("getBeatmap")
    operator fun get(index: Int): BeatmapInfo {
        return beatmaps[index]
    }

    override fun equals(other: Any?): Boolean {
        return other is BeatmapSetInfo && other.path == path
    }

}
