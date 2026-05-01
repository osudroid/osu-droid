package com.osudroid.multiplayer.api.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject


/**
 * Parse a [JSONObject] of player to [RoomPlayer].
 */
fun parsePlayer(o: JSONObject) = RoomPlayer(
    id = o.getString("id").toLong(),
    name = o.getString("username"),
    status = PlayerStatus[o.getInt("status")] ?: PlayerStatus.NotReady,
    team = if (o.isNull("team")) null else RoomTeam[o.getInt("team")],
    mods = RoomMods(o.getJSONArray("mods"))
)

/**
 * Parse a [JSONArray] of players to an array of [RoomPlayer]s of exactly [size] elements.
 *
 * `null` elements represent empty room slots.
 */
fun parsePlayers(array: JSONArray?, size: Int): Array<RoomPlayer?> {
    if (array != null) {
        val actual = array.length()

        when {
            actual > size -> Log.w(
                "Parsing",
                "parsePlayers: array has $actual entries but maxPlayers=$size; " +
                "${actual - size} excess player(s) beyond slot boundary will be ignored (EH-4)"
            )
            actual < size -> Log.d(
                "Parsing",
                "parsePlayers: array has $actual entries for a $size-slot room; " +
                "${size - actual} trailing slot(s) treated as empty (EH-4)"
            )
        }
    }

    return Array(size) { i ->
        parsePlayer(array?.optJSONObject(i) ?: return@Array null)
    }
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