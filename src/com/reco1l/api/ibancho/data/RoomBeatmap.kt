package com.reco1l.api.ibancho.data

data class RoomBeatmap
(
        val md5: String,
        val title: String?,
        val artist: String?,
        val creator: String?,
        val version: String?,
)
{

    var parentSetID: Long? = null

}
