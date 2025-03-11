package com.reco1l.osu.beatmaplisting.mirrors

import com.reco1l.osu.beatmaplisting.BeatmapMirrorDownloadRequestModel
import com.reco1l.osu.beatmaplisting.BeatmapMirrorPreviewRequestModel
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchRequestModel
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchRequestModel.*
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.*
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.*
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchResponseModel
import com.reco1l.osu.beatmaplisting.BeatmapModel
import com.reco1l.osu.beatmaplisting.BeatmapSetModel
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
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

        return MutableList(response.length()) { index ->
            val json = response.getJSONObject(index)

            BeatmapSetModel(
                id = json.getLong("id"),
                title = json.getString("title"),
                titleUnicode = json.getString("title_unicode"),
                artist = json.getString("artist"),
                artistUnicode = json.getString("artist_unicode"),
                status = RankedStatus.valueOf(json.getInt("ranked")),
                creator = json.getString("creator"),
                thumbnail = json.optJSONObject("covers")?.optString("card"),
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
    }
}

class OsuDirectDownloadRequestModel : BeatmapMirrorDownloadRequestModel {
    override fun invoke(beatmapSetId: Long): HttpUrl {
        return "https://osu.direct/api/d/$beatmapSetId".toHttpUrl()
    }
}

class OsuDirectPreviewRequestModel : BeatmapMirrorPreviewRequestModel {
    override fun invoke(beatmapId: Long): HttpUrl {
        return "https://osu.direct/api/media/preview/$beatmapId".toHttpUrl()
    }
}