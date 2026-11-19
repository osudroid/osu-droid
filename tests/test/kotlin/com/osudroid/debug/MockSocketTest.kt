package com.osudroid.debug

import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.MultiplayerClientEvent
import com.osudroid.multiplayer.api.MultiplayerServerEvent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.Config

@RunWith(RobolectricTestRunner::class)
class MockSocketTest {
    private lateinit var socket: MockSocket

    @Before
    fun setUp() {
        // Read by the mock server when the socket connects.
        Config.setOnlineUsername("tester")

        // The mock server continues numbering from the session.
        Multiplayer.session.reset()

        socket = MockSocket(1)
    }

    @Test
    fun `emit with an acknowledgement acknowledges once and does not pass it to listeners`() {
        val received = listenTo(MultiplayerServerEvent.ChatMessage)
        val acks = mutableListOf<List<Any?>>()

        socket.emit(MultiplayerClientEvent.ChatMessage.eventName, arrayOf("hi", "c1")) { acks.add(it.toList()) }

        assertEquals(1, acks.size)
        assertTrue((acks[0][0] as JSONObject).getBoolean("ok"))

        assertEquals(listOf(listOf<Any?>("1", "hi", 1)), received)
    }

    @Test
    fun `chat messages are echoed with increasing ids`() {
        val received = listenTo(MultiplayerServerEvent.ChatMessage)

        socket.emit(MultiplayerClientEvent.ChatMessage.eventName, "hi")
        socket.emit(MultiplayerClientEvent.ChatMessage.eventName, "again")

        assertEquals(listOf(listOf<Any?>("1", "hi", 1), listOf<Any?>("1", "again", 2)), received)
    }

    @Test
    fun `playBeatmap is echoed with an increasing round id`() {
        val received = listenTo(MultiplayerServerEvent.PlayBeatmap)

        socket.emit(MultiplayerClientEvent.PlayBeatmap.eventName)
        socket.emit(MultiplayerClientEvent.PlayBeatmap.eventName)

        assertEquals(listOf(listOf<Any?>(1), listOf<Any?>(2)), received)
    }

    @Test
    fun `final leaderboard carries the current round id`() {
        val received = listenTo(MultiplayerServerEvent.AllPlayersScoreSubmitted)

        socket.emit(MultiplayerClientEvent.PlayBeatmap.eventName)
        socket.emit(MultiplayerClientEvent.ScoreSubmission.eventName, JSONObject().put("score", 1))

        assertEquals(1, received.size)
        assertEquals(2, received[0].size)
        assertEquals(1, received[0][1])
    }

    @Test
    fun `initialConnection carries round zero and no chat history`() {
        val received = listenTo(MultiplayerServerEvent.InitialConnection)

        socket.connect()

        val json = received.single().single() as JSONObject

        assertEquals(0, json.getInt("roundId"))
        assertEquals(0, json.getInt("latestChatId"))
    }

    @Test
    fun `a new socket continues numbering from the session`() {
        Multiplayer.session.seedChat(5)
        Multiplayer.session.tryEnterLoading(3)

        val reconnected = MockSocket(1)
        val chat = mutableListOf<List<Any?>>()
        val rounds = mutableListOf<List<Any?>>()
        val connections = mutableListOf<JSONObject>()

        reconnected.on(MultiplayerServerEvent.ChatMessage.eventName) { chat.add(it.toList()) }
        reconnected.on(MultiplayerServerEvent.PlayBeatmap.eventName) { rounds.add(it.toList()) }
        reconnected.on(MultiplayerServerEvent.InitialConnection.eventName) { connections.add(it[0] as JSONObject) }

        reconnected.connect()
        reconnected.emit(MultiplayerClientEvent.ChatMessage.eventName, "hi")
        reconnected.emit(MultiplayerClientEvent.PlayBeatmap.eventName)

        assertEquals(5, connections.single().getInt("latestChatId"))
        assertEquals(3, connections.single().getInt("roundId"))
        assertEquals(listOf(listOf<Any?>("1", "hi", 6)), chat)
        assertEquals(listOf(listOf<Any?>(4)), rounds)
    }

    @Test
    fun `connected follows connect and disconnect`() {
        assertEquals(false, socket.connected())

        socket.connect()
        assertEquals(true, socket.connected())

        socket.disconnect()
        assertEquals(false, socket.connected())
    }

    private fun listenTo(event: MultiplayerServerEvent): List<List<Any?>> {
        val received = mutableListOf<List<Any?>>()

        socket.on(event.eventName) { received.add(it.toList()) }

        return received
    }
}
