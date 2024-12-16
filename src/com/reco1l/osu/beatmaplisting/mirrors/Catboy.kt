package com.reco1l.osu.beatmaplisting.mirrors

import com.reco1l.osu.beatmaplisting.BeatmapMirrorDownloadRequestModel
import com.reco1l.osu.beatmaplisting.BeatmapMirrorPreviewRequestModel
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchRequestModel
import com.reco1l.osu.beatmaplisting.BeatmapMirrorSearchResponseModel
import com.reco1l.osu.beatmaplisting.BeatmapModel
import com.reco1l.osu.beatmaplisting.BeatmapSetModel
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.RankedStatus

// API reference: https://dev.catboy.best/docs


class CatboySearchRequestModel : BeatmapMirrorSearchRequestModel {
    override fun invoke(query: String, offset: Int, limit: Int): HttpUrl {
        return "https://catboy.best/api/v2/search".toHttpUrl()
            .newBuilder()
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
    override fun invoke(beatmapSetId: Long): HttpUrl {
        return "https://catboy.best/preview/audio/$beatmapSetId?set=1".toHttpUrl()
    }
}
