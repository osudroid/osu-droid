package com.osudroid.beatmaplisting.mirrors

import com.osudroid.beatmaplisting.BeatmapMirrorBeatmapLinkHandler
import com.osudroid.beatmaplisting.BeatmapMirrorBeatmapsetLinkHandler
import com.osudroid.beatmaplisting.BeatmapMirrorDownloadRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorPreviewRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.Ascending
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.Descending
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.Artist
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.BPM
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.DifficultyRating
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.FavouriteCount
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.HitLength
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.LastUpdated
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.PassCount
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.PlayCount
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.RankedDate
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.SubmittedDate
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.Title
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.TotalLength
import com.osudroid.beatmaplisting.BeatmapMirrorSearchResponseModel
import com.osudroid.beatmaplisting.BeatmapModel
import com.osudroid.beatmaplisting.BeatmapSetModel
import com.osudroid.beatmaplisting.fetchMirrorJson
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.RankedStatus

// API reference: https://dev.catboy.best/docs


class CatboySearchRequestModel : BeatmapMirrorSearchRequestModel {

    private fun SortType.parseToApi() = when (this) {
        Title -> "title"
        Artist -> "artist"
        BPM -> "beatmaps.bpm"
        DifficultyRating -> "beatmaps.difficulty_rating"
        HitLength -> "beatmaps.hit_length"
        PassCount -> "beatmaps.passcount"
        PlayCount -> "beatmaps.playcount"
        TotalLength -> "beatmaps.total_length"
        FavouriteCount -> "favourite_count"
        LastUpdated -> "last_updated"
        RankedDate -> "ranked_date"
        SubmittedDate -> "submitted_date"
    }

    private fun OrderType.parseToApi() = when (this) {
        Ascending -> "asc"
        Descending -> "desc"
    }

    override fun invoke(query: String, offset: Int, limit: Int, sort: SortType, order: OrderType, status: RankedStatus?): HttpUrl {

        return "https://catboy.best/api/v2/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("sort", "${sort.parseToApi()}:${order.parseToApi()}")
            .apply {
                if (status != null) {
                    addQueryParameter("status", status.value.toString())
                }
            }
            .addQueryParameter("mode", "0")
            .addQueryParameter("query", query)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("offset", offset.toString())
            .build()
    }
}


class CatboySearchResponseModel : BeatmapMirrorSearchResponseModel {
    override fun invoke(response: Any): MutableList<BeatmapSetModel> {
        response as JSONArray

        return MutableList(response.length()) {
            parseCatboyBeatmapSet(response.getJSONObject(it))
        }.filter { it.beatmaps.isNotEmpty() }.toMutableList()
    }
}


class CatboyBeatmapLinkHandler(
    private val fetch: (HttpUrl) -> JSONObject = ::fetchMirrorJson
) : BeatmapMirrorBeatmapLinkHandler {
    override fun invoke(beatmapId: Long): BeatmapSetModel {
        return parseCatboyBeatmapSet(fetch("https://catboy.best/api/v2/b/$beatmapId".toHttpUrl()).getJSONObject("set"))
    }
}


class CatboyBeatmapsetLinkHandler(
    private val fetch: (HttpUrl) -> JSONObject = ::fetchMirrorJson
) : BeatmapMirrorBeatmapsetLinkHandler {
    override fun invoke(beatmapSetId: Long): BeatmapSetModel =
        parseCatboyBeatmapSet(fetch("https://catboy.best/api/v2/s/$beatmapSetId".toHttpUrl()))
}


/**
 * Parses a beatmap set JSON object returned by Catboy's API.
 */
private fun parseCatboyBeatmapSet(json: JSONObject): BeatmapSetModel {
    val id = json.getLong("id")

    return BeatmapSetModel(
        id = id,
        title = json.getString("title"),
        titleUnicode = if (json.isNull("title_unicode")) json.getString("title") else json.getString("title_unicode"),
        artist = json.getString("artist"),
        artistUnicode = if (json.isNull("artist_unicode")) json.getString("artist") else json.getString("artist_unicode"),
        status = RankedStatus.valueOf(json.getInt("ranked")),
        creator = json.getString("creator"),
        thumbnail = "https://assets.ppy.sh/beatmaps/${id}/covers/card.jpg",
        hasVideo = json.getBoolean("video"),
        beatmaps = json.getJSONArray("beatmaps").let {

            (0 until it.length()).map(it::getJSONObject).filter { obj -> obj.getInt("mode_int") == 0 }.map { obj ->

                BeatmapModel(
                    id = obj.getLong("id"),
                    version = obj.getString("version"),
                    starRating = obj.getDouble("difficulty_rating"),
                    ar = obj.getDouble("ar"),
                    cs = obj.getDouble("cs"),
                    hp = obj.getDouble("drain"),
                    od = obj.getDouble("accuracy"),
                    bpm = obj.getDouble("bpm"),
                    lengthSec = obj.getLong("hit_length"),
                    circleCount = obj.getInt("count_circles"),
                    sliderCount = obj.getInt("count_sliders"),
                    spinnerCount = obj.getInt("count_spinners")
                )

            }.sortedBy(BeatmapModel::starRating)
        }
    )
}


class CatboyDownloadRequestModel : BeatmapMirrorDownloadRequestModel {
    override fun invoke(beatmapSetId: Long, withVideo: Boolean): HttpUrl {
        return "https://catboy.best/d/$beatmapSetId".toHttpUrl()
    }
}


class CatboyPreviewRequestModel : BeatmapMirrorPreviewRequestModel {
    override fun invoke(beatmapId: Long): HttpUrl {
        return "https://catboy.best/preview/audio/$beatmapId".toHttpUrl()
    }
}
