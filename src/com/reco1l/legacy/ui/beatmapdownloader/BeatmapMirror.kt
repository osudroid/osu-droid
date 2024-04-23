package com.reco1l.legacy.ui.beatmapdownloader

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
     * [See documentation](https://old.osu.direct/doc)
     */
    OSU_DIRECT(
        search = MirrorAction(
            endpoint = "https://api.osu.direct/v2/search",
            mapResponse = { array ->

                MutableList(array.length()) { i ->

                    val it = array.getJSONObject(i)

                    BeatmapSetModel(
                        id = it.getLong("id"),
                        title = it.getString("title"),
                        titleUnicode = it.getString("title_unicode"),
                        artist = it.getString("artist"),
                        artistUnicode = it.getString("artist_unicode"),
                        status = RankedStatus.valueOf(it.getInt("ranked")),
                        creator = it.getString("creator"),
                        thumbnail = it.optJSONObject("covers")?.optString("card"),
                        beatmaps = run {

                            val beatmaps = mutableListOf<BeatmapModel>()
                            val array = it.getJSONArray("beatmaps")

                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)

                                beatmaps.add(
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
                                )
                            }

                            beatmaps.sortBy { it.starRating }

                            beatmaps
                        }
                    )
                }

            }
        ),
        downloadEndpoint = { "https://api.osu.direct/d/$it" },
        previewEndpoint = { "https://api.osu.direct/media/preview/$it" },
    );

}

