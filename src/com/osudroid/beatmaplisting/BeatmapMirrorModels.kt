package com.osudroid.beatmaplisting

import com.reco1l.framework.net.JsonObjectRequest
import com.reco1l.framework.net.UnsuccessfulResponseException
import com.reco1l.framework.net.WebRequest
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
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
     * @param withVideo Whether to include the video in the download if available.
     */
    operator fun invoke(beatmapSetId: Long, withVideo: Boolean): HttpUrl
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

fun interface BeatmapMirrorBeatmapLinkHandler {
    /**
     * Fetches the beatmap set containing the beatmap with the given ID.
     *
     * @param beatmapId The beatmap ID.
     * @return The beatmap set containing the beatmap.
     * @throws UnsuccessfulResponseException If the mirror responds with a non-2xx status.
     */
    operator fun invoke(beatmapId: Long): BeatmapSetModel
}

fun interface BeatmapMirrorBeatmapsetLinkHandler {
    /**
     * Fetches the beatmap set with the given ID.
     *
     * @param beatmapSetId The beatmap set ID.
     * @return The beatmap set.
     * @throws UnsuccessfulResponseException If the mirror responds with a non-2xx status.
     */
    operator fun invoke(beatmapSetId: Long): BeatmapSetModel
}


// Utilities

/**
 * Executes a GET [JsonObjectRequest] with the `User-Agent: Chrome/Android` header and returns its JSON.
 */
internal fun fetchMirrorJson(url: HttpUrl, client: OkHttpClient = WebRequest.globalClient): JSONObject =
    JsonObjectRequest(url).use { request ->
        request.client = client
        request.buildRequest { header("User-Agent", "Chrome/Android") }

        try {
            request.execute().json
        } catch (e: JSONException) {
            // The response body is parsed before the status code is checked, so a non-JSON error body (such as a
            // plain text 404) surfaces as a JSONException. Report it as an unsuccessful response instead.
            if (!request.response.isSuccessful) {
                throw UnsuccessfulResponseException(request.response).apply { addSuppressed(e) }
            }

            throw e
        }
    }
