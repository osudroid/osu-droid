package com.reco1l.ibancho.data

import com.reco1l.osu.multiplayer.stringToMods
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
    mods = parseMods(o.getJSONObject("mods"))
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
 * Parse a [JSONObject] of mods to [RoomMods]
 */
fun parseMods(o: JSONObject) = RoomMods(

    set = stringToMods(if (!o.isNull("mods")) o.getString("mods") else ""),
    speedMultiplier = o.getDouble("speedMultiplier").toFloat(),
    flFollowDelay = o.getDouble("flFollowDelay").toFloat(),

    customAR = if (!o.isNull("customAR")) o.getDouble("customAR").toFloat() else null,
    customOD = if (!o.isNull("customOD")) o.getDouble("customOD").toFloat() else null,
    customCS = if (!o.isNull("customCS")) o.getDouble("customCS").toFloat() else null,
    customHP = if (!o.isNull("customHP")) o.getDouble("customHP").toFloat() else null
)

/**
 * Parse a [JSONObject] of gameplay settings to [RoomGameplaySettings].
 */
fun parseGameplaySettings(o: JSONObject) = RoomGameplaySettings(
    isRemoveSliderLock = o.getBoolean("isRemoveSliderLock"),
    isFreeMod = o.getBoolean("isFreeMod"),
    allowForceDifficultyStatistics = o.getBoolean("allowForceDifficultyStatistics")
)