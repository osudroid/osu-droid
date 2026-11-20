package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.data.MultiplayerMessage
import com.osudroid.multiplayer.api.data.PlayerStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * The recovery data carried by the server's `initialConnection` event.
 *
 * @property snapshot The state used to reconcile the client with the server.
 * @property lastResultsScores The scores of the last finished round, as sent by the server, or `null` if there are none.
 * Non-null exactly when [MultiplayerRecoverySnapshot.lastResultsRoundId] of [snapshot] is.
 * @property recentChat The chat messages the client missed, oldest first.
 * @property latestChatId The ID of the latest chat message, or 0 if there is none.
 */
class MultiplayerRecoveryPayload(
    val snapshot: MultiplayerRecoverySnapshot,
    val lastResultsScores: JSONArray?,
    val recentChat: List<MultiplayerMessage>,
    val latestChatId: Int
)

/**
 * Converts this [JSONObject] to a [MultiplayerRecoveryPayload].
 *
 * @param clientStatus The status of this client in the room.
 * @return The parsed [MultiplayerRecoveryPayload].
 * @throws Exception If this [JSONObject] does not follow the protocol.
 */
fun JSONObject.toMultiplayerRecoveryPayload(clientStatus: PlayerStatus): MultiplayerRecoveryPayload {
    val round = optJSONObject("round")?.let {
        MultiplayerRoundProgress(
            allLoaded = it.getBoolean("allLoaded"),
            allSkipped = it.getBoolean("allSkipped"),
            loadReceived = it.getBoolean("loadReceived"),
            skipReceived = it.getBoolean("skipReceived"),
            scoreReceived = it.getBoolean("scoreReceived")
        )
    }

    val lastResults = optJSONObject("lastResults")

    val snapshot = MultiplayerRecoverySnapshot(
        roundId = getInt("roundId"),
        round = round,
        lastResultsRoundId = lastResults?.getInt("roundId"),
        clientStatus = clientStatus
    )

    return MultiplayerRecoveryPayload(
        snapshot = snapshot,
        lastResultsScores = lastResults?.getJSONArray("scores"),
        recentChat = optJSONArray("recentChat")?.parseRecentChat() ?: emptyList(),
        latestChatId = getInt("latestChatId")
    )
}

private fun JSONArray.parseRecentChat() = List(length()) { i ->
    val entry = getJSONObject(i)
    val id = entry.getInt("id")
    val message = entry.getString("message")

    if (entry.isNull("uid")) {
        MultiplayerMessage.System(id, message)
    } else {
        MultiplayerMessage.Player(id, entry.getString("uid").toLong(), message)
    }
}
