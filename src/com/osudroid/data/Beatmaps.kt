package com.osudroid.data

import android.util.*
import androidx.room.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.difficulty.BeatmapDifficultyCalculator
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.*
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import kotlin.math.max
import kotlin.math.min
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive


/// Ported from rimu! project

@Entity(
    primaryKeys = [
        "filename",
        "setDirectory"
    ],
    indices = [
        Index(name = "filenameIdx", value = ["filename"]),
        Index(name = "setDirectoryIdx", value = ["setDirectory"]),
        Index(name = "setIdx", value = ["setDirectory", "setId"])
    ]
)
data class BeatmapInfo(

    /**
     * The `.osu` filename.
     */
    val filename: String,

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
    var audioFilename: String,

    /**
     * The background filename.
     */
    var backgroundFilename: String?,


    // Online

    /**
     * The beatmap ranked status.
     */
    var status: Int?,


    // Parent set

    /**
     * The beatmap set directory relative to [Config.getBeatmapPath].
     */
    var setDirectory: String,

    /**
     * The beatmap set ID.
     */
    var setId: Int?,


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
     * The most common BPM.
     */
    var mostCommonBPM: Float,

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
    var maxCombo: Int,

    /**
     * Whether the beatmap has a epilepsy warning.
     */
    var epilepsyWarning: Boolean,

) {

    /**
     * The `.osu` file path.
     */
    val path
        get() = "${absoluteSetDirectory}/$filename"

    /**
     * The audio file path.
     */
    val audioPath
        get() = "${absoluteSetDirectory}/$audioFilename"

    /**
     * The background file path.
     */
    val backgroundPath
        get() = "${absoluteSetDirectory}/$backgroundFilename"

    /**
     * The beatmap set path ([setDirectory]) with [Config.getBeatmapPath] prepended.
     */
    val absoluteSetDirectory
        get() = "${Config.getBeatmapPath()}$setDirectory"

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
     * The full name of the beatmapset containing this beatmap without taking romanization into account.
     */
    val fullBeatmapsetName
        get() = buildString {
            if (setId != null && setId != -1) {
                append(setId)
                append(' ')
            }

            append(artist.takeUnless { it.isEmpty() } ?: "Unknown Artist")
            append(" - ")
            append(title.takeUnless { it.isEmpty() } ?: "Unknown Title")
        }

    /**
     * The full name of the beatmap without taking romanization into account.
     */
    val fullBeatmapName
        get() = buildString {
            append(artist.takeUnless { it.isEmpty() } ?: "Unknown Artist")
            append(" - ")
            append(title.takeUnless { it.isEmpty() } ?: "Unknown Title")
            append(" (")
            append(creator.takeUnless { it.isEmpty() } ?: "Unknown Creator")
            append(") [")
            append(version.takeUnless { it.isEmpty() } ?: "Unknown Version")
            append(']')
        }


    /**
     * Returns a [BeatmapDifficulty] based on the beatmap's difficulty properties.
     */
    fun getBeatmapDifficulty() = BeatmapDifficulty(circleSize, approachRate, overallDifficulty, hpDrainRate)

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

    fun apply(b: BeatmapInfo) {
        md5 = b.md5
        id = b.id
        audioFilename = b.audioFilename
        backgroundFilename = b.backgroundFilename
        status = b.status
        setDirectory = b.setDirectory
        setId = b.setId
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

    @JvmOverloads
    fun apply(beatmap: Beatmap, scope: CoroutineScope? = null) {
        md5 = beatmap.md5
        audioFilename = beatmap.general.audioFilename
        backgroundFilename = beatmap.events.backgroundFilename
        title = beatmap.metadata.title
        titleUnicode = beatmap.metadata.titleUnicode
        artist = beatmap.metadata.artist
        artistUnicode = beatmap.metadata.artistUnicode
        creator = beatmap.metadata.creator
        version = beatmap.metadata.version
        tags = beatmap.metadata.tags
        source = beatmap.metadata.source
        approachRate = beatmap.difficulty.ar
        overallDifficulty = beatmap.difficulty.od
        circleSize = beatmap.difficulty.gameplayCS
        hpDrainRate = beatmap.difficulty.hp
        length = beatmap.duration.toLong()
        previewTime = beatmap.general.previewTime
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount
        maxCombo = beatmap.maxCombo
        epilepsyWarning = beatmap.general.epilepsyWarning

        var bpmMin = Float.MAX_VALUE
        var bpmMax = 0f
        var bpmOverall = 0f
        var bpmOverallDuration = 0.0

        val timingPoints = beatmap.controlPoints.timing.controlPoints

        // The last playable time in the beatmap - the last timing point extends to this time.
        // Note: This is more accurate and may present different results because osu!stable didn't
        // have the ability to calculate slider durations in this context.
        val lastTime = beatmap.hitObjects.objects.lastOrNull()?.endTime ?: timingPoints.lastOrNull()?.time ?: 0.0

        timingPoints.fastForEachIndexed { i, t ->
            scope?.ensureActive()

            val bpm = t.bpm.toFloat()

            bpmMin = if (bpmMin != Float.MAX_VALUE) min(bpmMin, bpm) else bpm
            bpmMax = if (bpmMax != 0f) max(bpmMax, bpm) else bpm

            if (t.time > lastTime) {
                if (bpmOverall == 0f) {
                    bpmOverall = bpm
                    bpmOverallDuration = 0.0
                }

                return@fastForEachIndexed
            }

            // osu!stable forced the first control point to start at 0.
            val currentTime = if (i == 0) 0.0 else t.time
            val nextTime = if (i == timingPoints.size - 1) lastTime else timingPoints[i + 1].time
            val duration = nextTime - currentTime

            if (bpmOverall == 0f || bpmOverallDuration < duration) {
                bpmOverall = bpm
                bpmOverallDuration = duration
            }
        }

        if (bpmOverall == 0f) {
            bpmOverall = 60f
        }

        this.bpmMin = bpmMin
        this.bpmMax = bpmMax
        this.mostCommonBPM = bpmOverall
    }
}

/**
 * Represents a beatmap information.
 */
@JvmOverloads
fun BeatmapInfo(data: Beatmap, lastModified: Long, calculateDifficulty: Boolean, scope: CoroutineScope? = null): BeatmapInfo {
    var droidStarRating: Float? = null
    var standardStarRating: Float? = null

    if (calculateDifficulty) {
        try {
            val droidAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(data, scope = scope)
            val standardAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(data, scope = scope)

            droidStarRating = GameHelper.Round(droidAttributes.starRating, 2)
            standardStarRating = GameHelper.Round(standardAttributes.starRating, 2)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            Log.e("BeatmapInfo", "Error while calculating difficulty.", e)

            droidStarRating = 0f
            standardStarRating = 0f
        }
    }

    val beatmapInfo = BeatmapInfo(

        md5 = data.md5,
        id = data.metadata.beatmapId.toLong(),
        // The returned path is absolute. We only want the beatmap path relative to the beatmapset path.
        filename = data.filePath.substringAfterLast('/'),
        audioFilename = data.general.audioFilename,
        backgroundFilename = data.events.backgroundFilename,

        // Online
        status = null, // TODO: Should we cache ranking status ?

        // Parent set
        // The returned path is absolute. We only want the beatmapset path relative to the player-configured songs path.
        setDirectory = data.beatmapsetPath.substringAfterLast('/'),
        setId = data.metadata.beatmapSetId,

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
        circleSize = data.difficulty.gameplayCS,
        hpDrainRate = data.difficulty.hp,
        droidStarRating = droidStarRating,
        standardStarRating = standardStarRating,
        // These will be calculated in the apply call below
        bpmMin = 0f,
        bpmMax = 0f,
        mostCommonBPM = 0f,
        length = data.duration.toLong(),
        previewTime = data.general.previewTime,
        hitCircleCount = data.hitObjects.circleCount,
        sliderCount = data.hitObjects.sliderCount,
        spinnerCount = data.hitObjects.spinnerCount,
        maxCombo = data.maxCombo,
        epilepsyWarning = data.general.epilepsyWarning
    )

    beatmapInfo.apply(data, scope)

    return beatmapInfo
}

@Dao interface IBeatmapInfoDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(beatmapInfo: List<BeatmapInfo>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(beatmapInfo: BeatmapInfo)

    @Query("UPDATE BeatmapInfo SET droidStarRating = null")
    fun resetDroidStarRatings()

    @Query("UPDATE BeatmapInfo SET standardStarRating = null")
    fun resetStandardStarRatings()

    @Query("DELETE FROM BeatmapInfo WHERE setDirectory = :directory")
    fun deleteBeatmapSet(directory: String)

    @Query("DELETE FROM BeatmapInfo WHERE setDirectory IN (:directories)")
    fun deleteAllBeatmapSets(directories: List<String>)

    @Transaction
    @Query("SELECT DISTINCT setDirectory, setId FROM BeatmapInfo")
    fun getBeatmapSetList() : List<BeatmapSetInfo>

    @Query("SELECT DISTINCT setDirectory FROM BeatmapInfo")
    fun getBeatmapSetPaths() : List<String>

    @Query("SELECT EXISTS(SELECT setDirectory FROM BeatmapInfo WHERE setDirectory = :directory LIMIT 1)")
    fun isBeatmapSetImported(directory: String): Boolean

    @Query("DELETE FROM BeatmapInfo")
    fun deleteAll()
}


