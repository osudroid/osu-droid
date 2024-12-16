package com.reco1l.osu.beatmaplisting

import okhttp3.HttpUrl


// Request

fun interface BeatmapMirrorSearchRequestModel {
    /**
     * @param query The search query.
     * @param offset The search result offset.
     */
    operator fun invoke(query: String, offset: Int): HttpUrl
}

fun interface BeatmapMirrorDownloadRequestModel {
    /**
     * @param beatmapSetId The beatmap set ID.
     */
    operator fun invoke(beatmapSetId: Long): HttpUrl
}

fun interface BeatmapMirrorPreviewRequestModel {
    /**
     * @param beatmapSetId The beatmap set ID.
     */
    operator fun invoke(beatmapSetId: Long): HttpUrl
}


// Response

fun interface BeatmapMirrorSearchResponseModel {
    /**
     * @return The list of beatmap sets.
     */
    operator fun invoke(response: Any): MutableList<BeatmapSetModel>
}