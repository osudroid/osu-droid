package com.osudroid.beatmaplisting

import androidx.annotation.DrawableRes
import com.osudroid.beatmaplisting.mirrors.CatboyBeatmapLinkHandler
import com.osudroid.beatmaplisting.mirrors.CatboyBeatmapsetLinkHandler
import com.osudroid.beatmaplisting.mirrors.CatboyDownloadRequestModel
import com.osudroid.beatmaplisting.mirrors.CatboyPreviewRequestModel
import com.osudroid.beatmaplisting.mirrors.CatboySearchRequestModel
import com.osudroid.beatmaplisting.mirrors.CatboySearchResponseModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectBeatmapLinkHandler
import com.osudroid.beatmaplisting.mirrors.OsuDirectBeatmapsetLinkHandler
import com.osudroid.beatmaplisting.mirrors.OsuDirectDownloadRequestModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectPreviewRequestModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectSearchRequestModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectSearchResponseModel
import ru.nsu.ccfit.zuev.osuplus.R

/**
 * Defines a beatmap mirror API and its actions.
 */
enum class BeatmapMirror(

    /**
     * The home URL of the beatmap mirror where the user will be redirected to when
     * clicking on the logo.
     */
    val homeUrl: String,

    /**
     * The description of the beatmap mirror.
     */
    val description: String,

    /**
     * The resource ID of the logo image to be displayed in the UI.
     */
    @field:DrawableRes
    val logoResource: Int,

    /**
     * Whether this beatmap mirror supports downloading beatmaps without video. If `false`, the download action will
     * always include the video if available.
     */
    val supportsNoVideoDownloads: Boolean,

    // Actions / Endpoints

    /**
     * The search query action.
     */
    val search: BeatmapMirrorActionWithResponse<BeatmapMirrorSearchRequestModel, BeatmapMirrorSearchResponseModel>,

    /**
     * The download action.
     */
    val download: BeatmapMirrorAction<BeatmapMirrorDownloadRequestModel>,

    /**
     * The music preview action.
     */
    val preview: BeatmapMirrorAction<BeatmapMirrorPreviewRequestModel>,

    /**
     * Fetches the beatmap set containing a given beatmap.
     */
    val handleBeatmapLink: BeatmapMirrorBeatmapLinkHandler,

    /**
     * Fetches a beatmap set by its ID.
     */
    val handleBeatmapsetLink: BeatmapMirrorBeatmapsetLinkHandler,

) {

    /**
     * osu.direct beatmap mirror.
     *
     * [See documentation](https://osu.direct/api/docs)
     */
    OSU_DIRECT(
        homeUrl = "https://osu.direct",
        description = "osu.direct",
        logoResource = R.drawable.osudirect,
        supportsNoVideoDownloads = true,

        search = BeatmapMirrorActionWithResponse(
            request = OsuDirectSearchRequestModel(),
            response = OsuDirectSearchResponseModel(),
        ),
        download = BeatmapMirrorAction(OsuDirectDownloadRequestModel()),
        preview = BeatmapMirrorAction(OsuDirectPreviewRequestModel()),
        handleBeatmapLink = OsuDirectBeatmapLinkHandler(),
        handleBeatmapsetLink = OsuDirectBeatmapsetLinkHandler(),
    ),

    /**
     * Catboy beatmap mirror.
     *
     * [See documentation](https://dev.catboy.best/docs)
     */
    CATBOY(
        homeUrl = "https://catboy.best",
        description = "Mino",
        logoResource = R.drawable.catboy,
        supportsNoVideoDownloads = false,

        search = BeatmapMirrorActionWithResponse(
            request = CatboySearchRequestModel(),
            response = CatboySearchResponseModel(),
        ),
        download = BeatmapMirrorAction(CatboyDownloadRequestModel()),
        preview = BeatmapMirrorAction(CatboyPreviewRequestModel()),
        handleBeatmapLink = CatboyBeatmapLinkHandler(),
        handleBeatmapsetLink = CatboyBeatmapsetLinkHandler(),
    )

}

