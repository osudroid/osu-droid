package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.data.MultiplayerMessage
import com.osudroid.multiplayer.api.data.PlayerStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultiplayerRecoveryPayloadTest {
    @Test
    fun `parses every recovery field`() {
        val json = JSONObject(
            """
            {
                "id": "room",
                "roundId": 7,
                "round": {
                    "allLoaded": true,
                    "allSkipped": false,
                    "loadReceived": true,
                    "skipReceived": false,
                    "scoreReceived": true
                },
                "lastResults": { "roundId": 6, "scores": [ { "username": "a" }, { "username": "b" } ] },
                "recentChat": [
                    { "id": 11, "uid": "100", "message": "hello" },
                    { "id": 12, "uid": null, "message": "system message" }
                ],
                "latestChatId": 12
            }
            """.trimIndent()
        )

        val payload = json.toMultiplayerRecoveryPayload(PlayerStatus.Playing)

        assertEquals(
            MultiplayerRecoverySnapshot(
                roundId = 7,
                round = MultiplayerRoundProgress(
                    allLoaded = true,
                    allSkipped = false,
                    loadReceived = true,
                    skipReceived = false,
                    scoreReceived = true
                ),
                lastResultsRoundId = 6,
                clientStatus = PlayerStatus.Playing
            ),
            payload.snapshot
        )

        val scores = payload.lastResultsScores!!
        assertEquals(2, scores.length())
        assertEquals("b", scores.getJSONObject(1).getString("username"))

        assertEquals(
            listOf(
                MultiplayerMessage.Player(11, 100L, "hello"),
                MultiplayerMessage.System(12, "system message")
            ),
            payload.recentChat
        )
        assertEquals(12, payload.latestChatId)
    }

    @Test
    fun `omitted optional fields are absent`() {
        val json = JSONObject("""{ "id": "room", "roundId": 3, "latestChatId": 0 }""")
        val payload = json.toMultiplayerRecoveryPayload(PlayerStatus.Ready)

        assertEquals(
            MultiplayerRecoverySnapshot(roundId = 3, round = null, lastResultsRoundId = null, clientStatus = PlayerStatus.Ready),
            payload.snapshot
        )
        assertNull(payload.lastResultsScores)
        assertTrue(payload.recentChat.isEmpty())
        assertEquals(0, payload.latestChatId)
    }

    @Test
    fun `payload that does not follow the protocol throws`() {
        val invalid = listOf(
            """{ "latestChatId": 0 }""",
            """{ "roundId": 1 }""",
            """{ "roundId": 1, "latestChatId": 0, "round": { "allLoaded": true } }""",
            """{ "roundId": 1, "latestChatId": 0, "lastResults": { "roundId": 1 } }""",
            """{ "roundId": 1, "latestChatId": 0, "lastResults": { "scores": [] } }""",
            """{ "roundId": 1, "latestChatId": 1, "recentChat": [ { "uid": null, "message": "m" } ] }""",
            """{ "roundId": 1, "latestChatId": 1, "recentChat": [ { "id": 1, "uid": "nope", "message": "m" } ] }"""
        )

        for (payload in invalid) {
            assertThrows(payload, Exception::class.java) {
                JSONObject(payload).toMultiplayerRecoveryPayload(PlayerStatus.Ready)
            }
        }
    }
}
