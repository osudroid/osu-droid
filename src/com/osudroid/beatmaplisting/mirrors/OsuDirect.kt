package com.osudroid.beatmaplisting.mirrors

import com.osudroid.beatmaplisting.BeatmapMirrorBeatmapLinkHandler
import com.osudroid.beatmaplisting.BeatmapMirrorBeatmapsetLinkHandler
import com.osudroid.beatmaplisting.BeatmapMirrorDownloadRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorPreviewRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchResponseModel
import com.osudroid.beatmaplisting.BeatmapModel
import com.osudroid.beatmaplisting.BeatmapSetModel
import com.osudroid.beatmaplisting.fetchMirrorJson
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.RankedStatus


class OsuDirectSearchRequestModel : BeatmapMirrorSearchRequestModel {

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

        return "https://osu.direct/api/v2/search".toHttpUrl()
            .newBuilder()
            .addQueryParameter("sort", "${sort.parseToApi()}:${order.parseToApi()}")
            .apply {
                if (status != null) {
                    addQueryParameter("status", status.value.toString())
                }
            }
            .addQueryParameter("mode", "0")
            .addQueryParameter("query", query)
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("amount", limit.toString())
            .build()
    }
}

class OsuDirectSearchResponseModel : BeatmapMirrorSearchResponseModel {
    override fun invoke(response: Any): MutableList<BeatmapSetModel> {
        response as JSONArray

        return MutableList(response.length()) { parseOsuDirectBeatmapSet(response.getJSONObject(it)) }
    }
}

class OsuDirectBeatmapLinkHandler(
    private val fetch: (HttpUrl) -> JSONObject = ::fetchMirrorJson
) : BeatmapMirrorBeatmapLinkHandler {
    override fun invoke(beatmapId: Long): BeatmapSetModel {
        val beatmap = fetch("https://osu.direct/api/v2/b/$beatmapId".toHttpUrl())

        return parseOsuDirectBeatmapSet(fetch("https://osu.direct/api/v2/s/${beatmap.getLong("beatmapset_id")}".toHttpUrl()))
    }
}


class OsuDirectBeatmapsetLinkHandler(
    private val fetch: (HttpUrl) -> JSONObject = ::fetchMirrorJson
) : BeatmapMirrorBeatmapsetLinkHandler {
    override fun invoke(beatmapSetId: Long): BeatmapSetModel =
        parseOsuDirectBeatmapSet(fetch("https://osu.direct/api/v2/s/$beatmapSetId".toHttpUrl()))
}

/**
 * Parses a beatmap set JSON object returned by OsuDirect's API.
 */
private fun parseOsuDirectBeatmapSet(json: JSONObject): BeatmapSetModel {
    return BeatmapSetModel(
        id = json.getLong("id"),
        title = json.getString("title"),
        titleUnicode = if (json.isNull("title_unicode")) json.getString("title") else json.getString("title_unicode"),
        artist = json.getString("artist"),
        artistUnicode = if (json.isNull("artist_unicode")) json.getString("artist") else json.getString("artist_unicode"),
        status = RankedStatus.valueOf(json.getInt("ranked")),
        creator = json.getString("creator"),
        thumbnail = json.optJSONObject("covers")?.optString("card"),
        hasVideo = json.getBoolean("video"),
        beatmaps = json.getJSONArray("beatmaps").let {

            MutableList(it.length()) { i ->

                val obj = it.getJSONObject(i)

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

class OsuDirectDownloadRequestModel : BeatmapMirrorDownloadRequestModel {
    override fun invoke(beatmapSetId: Long, withVideo: Boolean): HttpUrl {
        val url = "https://osu.direct/api/d/$beatmapSetId".toHttpUrl()

        return if (!withVideo) url.newBuilder().addQueryParameter("noVideo", "1").build() else url
    }
}

class OsuDirectPreviewRequestModel : BeatmapMirrorPreviewRequestModel {
    override fun invoke(beatmapId: Long): HttpUrl {
        return "https://osu.direct/api/media/preview/$beatmapId".toHttpUrl()
    }
}