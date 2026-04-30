package com.osudroid.multiplayer.api.data

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject


/**
 * Parse a [JSONObject] of player to [RoomPlayer]
 */
fun parsePlayer(o: JSONObject) = RoomPlayer(
    id = o.getString("id").toLong(),
    name = o.getString("username"),
    // Unknown PlayerStatus ordinals (EH-1) fall back to NotReady so the player is visible
    // but not ready, which is the safest state.
    status = PlayerStatus[o.getInt("status")] ?: PlayerStatus.NotReady,
    // Unknown RoomTeam ordinals return null; team is nullable so no fallback needed.
    team = if (o.isNull("team")) null else o.getInt("team").let { n -> RoomTeam[n] },
    mods = RoomMods(o.getJSONArray("mods"))
)

/**
 * Parse a [JSONArray] of players to an array of [RoomPlayer?] of exactly [size] elements.
 *
 * Null elements represent empty room slots.  Any mismatch between [array]'s actual length and
 * [size] is logged (EH-4):
 * - `array.length() > size` — excess entries are silently dropped (server bug / API mismatch).
 * - `array.length() < size` — trailing slots become null (normal for partially-filled rooms,
 *   but logged at DEBUG level so unexpected under-sends are observable).
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