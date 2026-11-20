package com.osudroid.multiplayer.api

import com.osudroid.debug.MockSocket
import com.osudroid.multiplayer.session.MultiplayerAckResult
import com.osudroid.multiplayer.session.MultiplayerOutbox
import io.socket.client.Ack
import io.socket.client.AckWithTimeout
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.Config

@RunWith(RobolectricTestRunner::class)
class MultiplayerSocketTransportTest {
    private val socket = MockSocket(1)
    private var current: Socket? = socket
    private val transport = MultiplayerSocketTransport { current }

    private val received = mutableListOf<List<Any?>>()
    private val acks = mutableListOf<MultiplayerAckResult?>()

    @Before
    fun setUp() {
        // Read by the mock server when the socket connects.
        Config.setOnlineUsername("tester")

        socket.on(MultiplayerClientEvent.BeatmapChanged.eventName) { received.add(it.toList()) }
    }

    @Test
    fun `emit before the session is established reports no acknowledgement and sends nothing`() {
        socket.connect()

        assertFalse(transport.isReady)

        emitBeatmapChanged(JSONObject())

        assertEquals(listOf<MultiplayerAckResult?>(null), acks)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `emit while the socket is not connected reports no acknowledgement and sends nothing`() {
        transport.establishedSocket = socket

        assertFalse(transport.isReady)

        emitBeatmapChanged(JSONObject())

        assertEquals(listOf<MultiplayerAckResult?>(null), acks)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `emit without a socket reports no acknowledgement`() {
        current = null
        transport.establishedSocket = socket

        assertFalse(transport.isReady)

        emitBeatmapChanged(JSONObject())

        assertEquals(listOf<MultiplayerAckResult?>(null), acks)
    }

    @Test
    fun `emit while established and connected sends the event and reports the acknowledgement`() {
        socket.connect()
        transport.establishedSocket = socket

        assertTrue(transport.isReady)

        val json = JSONObject().put("md5", "abc")
        emitBeatmapChanged(json)

        assertEquals(listOf<List<Any?>>(listOf(json)), received)
        assertEquals(listOf<MultiplayerAckResult?>(MultiplayerAckResult(true, null)), acks)
    }

    @Test
    fun `isReady is false again after the socket disconnects`() {
        socket.connect()
        transport.establishedSocket = socket
        socket.disconnect()

        assertFalse(transport.isReady)
    }

    @Test
    fun `a session established on a replaced socket does not make the current socket ready`() {
        val replacement = MockSocket(1)
        replacement.on(MultiplayerClientEvent.BeatmapChanged.eventName) { received.add(it.toList()) }

        socket.connect()
        replacement.connect()
        transport.establishedSocket = socket
        current = replacement

        assertFalse(transport.isReady)

        emitBeatmapChanged(JSONObject())

        assertEquals(listOf<MultiplayerAckResult?>(null), acks)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `an outbox holds an event while the session is established on a disconnected socket`() {
        val outbox = MultiplayerOutbox(transport)

        transport.establishedSocket = socket

        outbox.sendUntilAcknowledged(MultiplayerClientEvent.BeatmapLoadComplete, 1, arrayOf())

        assertEquals(listOf(MultiplayerClientEvent.BeatmapLoadComplete), outbox.unacknowledged)
        assertTrue(received.isEmpty())
    }

    @Test
    fun `an outbox holds an event for a disconnected socket and sends it on flush after reconnecting`() {
        val outbox = MultiplayerOutbox(transport)

        socket.connect()
        transport.establishedSocket = socket
        socket.disconnect()

        outbox.sendUntilAcknowledged(MultiplayerClientEvent.BeatmapChanged, null, arrayOf(JSONObject()))

        assertEquals(listOf(MultiplayerClientEvent.BeatmapChanged), outbox.unacknowledged)

        socket.connect()
        outbox.flush()

        assertEquals(1, received.size)
        assertTrue(outbox.unacknowledged.isEmpty())
    }

    @Test
    fun `the acknowledgement handed to the socket parses responses and reports timeouts`() {
        val capturing = object : Socket(null, null, null) {
            var ack: Ack? = null

            override fun connected() = true

            override fun emit(event: String?, args: Array<Any?>?, ack: Ack?): Emitter {
                this.ack = ack
                return this
            }
        }

        current = capturing
        transport.establishedSocket = capturing

        emitBeatmapChanged(JSONObject())

        val ack = capturing.ack as AckWithTimeout

        ack.onSuccess("nope")
        ack.onTimeout()

        assertEquals(listOf(MultiplayerAckResult(false, "Malformed acknowledgement"), null), acks)
        assertNull(acks[1])
    }

    @Test
    fun `parseMultiplayerAckResult reads an accepted event`() {
        assertEquals(
            MultiplayerAckResult(true, null),
            parseMultiplayerAckResult(arrayOf(JSONObject().put("ok", true)))
        )
    }

    @Test
    fun `parseMultiplayerAckResult reads a rejected event with its reason`() {
        assertEquals(
            MultiplayerAckResult(false, "x"),
            parseMultiplayerAckResult(arrayOf(JSONObject().put("ok", false).put("reason", "x")))
        )
    }

    @Test
    fun `parseMultiplayerAckResult reports malformed acknowledgements as rejected`() {
        val malformed = MultiplayerAckResult(false, "Malformed acknowledgement")

        assertEquals(malformed, parseMultiplayerAckResult(arrayOf("nope")))
        assertEquals(malformed, parseMultiplayerAckResult(arrayOf(JSONObject())))
        assertEquals(malformed, parseMultiplayerAckResult(arrayOf()))
    }

    private fun emitBeatmapChanged(json: JSONObject) =
        transport.emit(MultiplayerClientEvent.BeatmapChanged, arrayOf(json), 1_000L) { acks.add(it) }
}
