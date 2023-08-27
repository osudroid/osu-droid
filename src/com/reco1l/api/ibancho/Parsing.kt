package com.reco1l.api.ibancho

import com.reco1l.api.ibancho.data.PlayerStatus
import com.reco1l.api.ibancho.data.RoomBeatmap
import com.reco1l.api.ibancho.data.RoomPlayer
import com.reco1l.api.ibancho.data.RoomTeam
import com.reco1l.legacy.data.stringToMods
import com.reco1l.legacy.ui.multiplayer.RoomMods
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parse a [JSONObject] of player to [RoomPlayer]
 */
internal fun parsePlayer(o: JSONObject): RoomPlayer
{
    return RoomPlayer(
            id = o.getString("uid").toLong(),
            name = o.getString("username"),
            status = PlayerStatus.from(o.getInt("status")),
            team = if (o.isNull("team")) null else o.getInt("team").let { n -> RoomTeam.from(n) },
            mods = parseMods(o.getJSONObject("mods"))
    )
}

/**
 * Parse a [JSONArray] of players to [RoomPlayer]
 */
internal fun parsePlayers(array: JSONArray?, max: Int) = Array(max) { i ->

    val o = array?.optJSONObject(i) ?: return@Array null

    parsePlayer(o)
}

/**
 * Parse a [JSONObject] of `beatmap` to [RoomBeatmap]
 */
internal fun parseBeatmap(o: JSONObject?): RoomBeatmap?
{
    if (o == null || !o.has("md5")) return null

    return RoomBeatmap(
            md5 = o.getString("md5"),
            title = o.optString("title"),
            artist = o.optString("artist"),
            creator = o.optString("creator"),
            version = o.optString("version")
    ).apply {

        if (!o.isNull("beatmapSetId"))
            parentSetID = o.getString("beatmapSetId").toLong()
    }
}

/**
 * Parse a [JSONObject] of mods to [RoomMods]
 */
internal fun parseMods(o: JSONObject): RoomMods
{
    return RoomMods(
        set = stringToMods(if (!o.isNull("mods")) o.getString("mods") else ""),
        speedMultiplier = o.getDouble("speedMultiplier").toFloat(),
        flFollowDelay = o.getDouble("flFollowDelay").toFloat(),
        forceAR = if (!o.isNull("forceAR")) o.getDouble("forceAR").toFloat() else null
    )
}