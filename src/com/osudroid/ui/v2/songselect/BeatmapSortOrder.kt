package com.osudroid.ui.v2.songselect

import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm

/**
 * The order of the beatmap sets in the [BeatmapCarrousel].
 *
 * Stored by ordinal in the same preference as the order of the legacy song menu, the entries must keep their order.
 */
enum class BeatmapSortOrder(val label: String, val comparator: Comparator<BeatmapSetModel>) {

    Title("Title", compareBy(String.CASE_INSENSITIVE_ORDER) { it.firstBeatmap?.titleText.orEmpty() }),
    Artist("Artist", compareBy(String.CASE_INSENSITIVE_ORDER) { it.firstBeatmap?.artistText.orEmpty() }),
    Creator("Creator", compareBy(String.CASE_INSENSITIVE_ORDER) { it.firstBeatmap?.creator.orEmpty() }),
    Date("Date", compareByDescending { it.firstBeatmap?.dateImported ?: 0L }),
    Bpm("BPM", compareByDescending { it.firstBeatmap?.bpmMax ?: 0f }),

    // By the hardest difficulty of the set. Both are listed as one order, which of them applies depends on the
    // difficulty algorithm.
    DroidStars("Stars", compareByDescending { model ->
        model.beatmapSetInfo.beatmaps.maxOfOrNull { it.getStarRating(DifficultyAlgorithm.droid) } ?: 0f
    }),
    StandardStars("Stars", compareByDescending { model ->
        model.beatmapSetInfo.beatmaps.maxOfOrNull { it.getStarRating(DifficultyAlgorithm.standard) } ?: 0f
    }),

    Length("Length", compareByDescending { it.firstBeatmap?.length ?: 0L })

}

private val BeatmapSetModel.firstBeatmap
    get() = beatmapSetInfo.beatmaps.getOrNull(0)
