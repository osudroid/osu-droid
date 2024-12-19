package com.reco1l.osu.beatmaplisting

import ru.nsu.ccfit.zuev.osu.RankedStatus

/**
 * Beatmap set response model for beatmaps mirrors.
 */
data class BeatmapSetModel(
    val id: Long,
    val title: String,
    val titleUnicode: String,
    val artist: String,
    val artistUnicode: String,
    val status: RankedStatus,
    val creator: String,
    val thumbnail: String?,
    val beatmaps: List<BeatmapModel>
)

/**
 * Beatmap response model for beatmaps mirrors.
 */
data class BeatmapModel(
    val id: Long,
    val version: String,
    val starRating: Double,
    val ar: Double,
    val cs: Double,
    val hp: Double,
    val od: Double,
    val bpm: Double,
    val lengthSec: Long,
    val circleCount: Int,
    val sliderCount:Int,
    val spinnerCount: Int
)