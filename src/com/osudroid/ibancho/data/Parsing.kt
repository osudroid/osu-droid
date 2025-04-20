package com.osudroid.ibancho.data

import org.json.JSONArray
import org.json.JSONObject


/**
 * Parse a [JSONObject] of player to [RoomPlayer]
 */
fun parsePlayer(o: JSONObject) = RoomPlayer(
    id = o.getString("uid").toLong(),
    name = o.getString("username"),
    status = PlayerStatus[o.getInt("status")],
    team = if (o.isNull("team")) null else o.getInt("team").let { n -> RoomTeam[n] },
    mods = RoomMods(o.getJSONArray("mods"))
)

/**
 * Parse a [JSONArray] of players to [RoomPlayer]
 */
fun parsePlayers(array: JSONArray?, size: Int) = Array(size) { i ->
    parsePlayer(array?.optJSONObject(i) ?: return@Array null)
}

/**
 * Parse a [JSONObject] of `beatmap` to [RoomBeatmap]
 */
fun parseBeatmap(o: JSONObject?): RoomBeatmap? {

    if (o == null || !o.has("md5")) {
        return null
    }

    val beatmap = RoomBeatmap(
        md5 = o.getString("md5"),
        title = o.optString("title"),
        artist = o.optString("artist"),
        creator = o.optString("creator"),
        version = o.optString("version")
    )

    if (!o.isNull("beatmapSetId")) {
        beatmap.parentSetID = o.getString("beatmapSetId").toLong()
    }

    return beatmap
}

/**
 * Parse a [JSONObject] of gameplay settings to [RoomGameplaySettings].
 */
fun parseGameplaySettings(o: JSONObject) = RoomGameplaySettings(
    isRemoveSliderLock = o.getBoolean("isRemoveSliderLock"),
    isFreeMod = o.getBoolean("isFreeMod")
)