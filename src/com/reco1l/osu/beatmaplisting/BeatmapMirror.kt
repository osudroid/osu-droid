package com.reco1l.osu.beatmaplisting

import org.json.JSONArray
import ru.nsu.ccfit.zuev.osu.RankedStatus


/**
 * Defines an action to be performed on a mirror API.
 */
data class MirrorAction<R, M>(

    /**
     * The action API endpoint.
     */
    // TODO replace with a request creation function, some APIs have different query arguments.
    val endpoint: String,

    /**
     * A function to map the response into a model.
     */
    val mapResponse: (R) -> M

)

/**
 * Defines a beatmap mirror API and its actions.
 */
enum class BeatmapMirror(

    /**
     * The search query action.
     */
    val search: MirrorAction<JSONArray, MutableList<BeatmapSetModel>>,

    val downloadEndpoint: (Long) -> String,

    val previewEndpoint: (Long) -> String,

    ) {

    /**
     * osu.direct beatmap mirror.
     *
     * [See documentation](https://osu.direct/api/docs)
     */
    OSU_DIRECT(
        search = MirrorAction(
            endpoint = "https://osu.direct/api/v2/search",
            mapResponse = { array ->

                MutableList(array.length()) { index ->

                    val json = array.getJSONObject(index)

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
        ),
        downloadEndpoint = { "https://osu.direct/api/d/$it" },
        previewEndpoint = { "https://osu.direct/api/media/preview/$it" },
    );

}

