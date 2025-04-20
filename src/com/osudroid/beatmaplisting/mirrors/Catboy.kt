package com.osudroid.beatmaplisting.mirrors

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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
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

        return MutableList(response.length()) { i ->
            val json = response.getJSONObject(i)

            BeatmapSetModel(
                id = json.getLong("id"),
                title = json.getString("title"),
                titleUnicode = json.getString("title_unicode"),
                artist = json.getString("artist"),
                artistUnicode = json.getString("artist_unicode"),
                status = RankedStatus.valueOf(json.getInt("ranked")),
                creator = json.getString("creator"),
                thumbnail = "https://assets.ppy.sh/beatmaps/${json.getLong("id")}/covers/card.jpg",
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


class CatboyDownloadRequestModel : BeatmapMirrorDownloadRequestModel {
    override fun invoke(beatmapSetId: Long): HttpUrl {
        return "https://catboy.best/d/$beatmapSetId".toHttpUrl()
    }
}


class CatboyPreviewRequestModel : BeatmapMirrorPreviewRequestModel {
    override fun invoke(beatmapId: Long): HttpUrl {
        return "https://catboy.best/preview/audio/$beatmapId".toHttpUrl()
    }
}
