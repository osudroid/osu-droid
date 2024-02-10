package com.rian.osu.beatmap.sections

/**
 * Contains information used to identify a beatmap.
 */
data class BeatmapMetadata(
    /**
     * The romanized song title of this beatmap.
     */
    @JvmField
    var title: String = "",

    /**
     * The song title of this beatmap.
     */
    @JvmField
    var titleUnicode: String = "",

    /**
     * The romanized artist of the song of this beatmap.
     */
    @JvmField
    var artist: String = "",

    /**
     * The song artist of this beatmap.
     */
    @JvmField
    var artistUnicode: String = "",

    /**
     * The creator of this beatmap.
     */
    @JvmField
    var creator: String = "",

    /**
     * The difficulty name of this beatmap.
     */
    @JvmField
    var version: String = "",

    /**
     * The original media the song was produced for.
     */
    @JvmField
    var source: String = "",

    /**
     * The search terms of this beatmap.
     */
    @JvmField
    var tags: String = "",

    /**
     * The ID of this beatmap.
     */
    @JvmField
    var beatmapID: Int = -1,

    /**
     * The ID of this beatmap set containing this beatmap.
     */
    @JvmField
    var beatmapSetID: Int = -1
)
