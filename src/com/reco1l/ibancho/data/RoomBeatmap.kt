package com.reco1l.ibancho.data

/**
 * Represents a beatmap model in a multiplayer room.
 */
data class RoomBeatmap(

    /**
     * The MD5 hash of the beatmap.
     */
    val md5: String,

    /**
     * The title in unicode of the beatmap.
     */
    val title: String?,

    /**
     * The artist in unicode of the beatmap.
     */
    val artist: String?,

    /**
     * The creator of the beatmap.
     */
    val creator: String?,

    /**
     * The version of the beatmap.
     */
    val version: String?,
) {

    /**
     * The parent set ID of the beatmap.
     */
    var parentSetID: Long? = null

}
