package com.reco1l.osu.beatmaplisting

import okhttp3.HttpUrl
import ru.nsu.ccfit.zuev.osu.RankedStatus


// Request

fun interface BeatmapMirrorSearchRequestModel {
    /**
     * @param query The search query.
     * @param offset The search result offset.
     */
    operator fun invoke(query: String, offset: Int, limit: Int, sort: SortType, order: OrderType, status: RankedStatus?): HttpUrl

    /**
     * Sorting types availables by the osu!api.
     */
    enum class SortType(val description: String) {
        Title("Title"),
        Artist("Artist"),
        BPM("BPM"),
        DifficultyRating("Difficulty rating"),
        HitLength("Hit length"),
        PassCount("Pass count"),
        PlayCount("Play count"),
        TotalLength("Total length"),
        FavouriteCount("Favourite count"),
        LastUpdated("Last updated"),
        RankedDate("Ranked date"),
        SubmittedDate("Submitted date")
    }

    enum class OrderType(val description: String) {
        Ascending("Ascending"),
        Descending("Descending"),
    }

}

fun interface BeatmapMirrorDownloadRequestModel {
    /**
     * @param beatmapSetId The beatmap set ID.
     */
    operator fun invoke(beatmapSetId: Long): HttpUrl
}

fun interface BeatmapMirrorPreviewRequestModel {
    /**
     * @param beatmapId The beatmap ID.
     */
    operator fun invoke(beatmapId: Long): HttpUrl
}


// Response

fun interface BeatmapMirrorSearchResponseModel {
    /**
     * @return The list of beatmap sets.
     */
    operator fun invoke(response: Any): MutableList<BeatmapSetModel>
}