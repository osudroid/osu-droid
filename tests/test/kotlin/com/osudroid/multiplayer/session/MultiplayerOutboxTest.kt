package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.MultiplayerClientEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiplayerOutboxTest {
    @Test
    fun `sendUntilAcknowledged stops tracking acknowledged event`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))

        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission), outbox.unacknowledged)

        transport.emissions.single().onAck(okAckResult)

        assertTrue(outbox.unacknowledged.isEmpty())
    }

    @Test
    fun `sendUntilAcknowledged stops tracking rejected event`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.SkipRequested, 3, emptyArray())

        transport.emissions.single().onAck(MultiplayerAckResult(false, "not playing"))

        assertTrue(outbox.unacknowledged.isEmpty())
    }

    @Test
    fun `sendUntilAcknowledged appends round ID to round-scoped event`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))

        val emission = transport.emissions.single()

        assertEquals(MultiplayerClientEvent.ScoreSubmission, emission.event)
        assertEquals(listOf<Any?>("json", 3), emission.args.toList())
        assertEquals(ACK_TIMEOUT_MS, emission.timeoutMs)
    }

    @Test
    fun `sendUntilAcknowledged omits null round ID`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.BeatmapLoadComplete, null, emptyArray())

        assertEquals(emptyList<Any?>(), transport.emissions.single().args.toList())
    }

    @Test
    fun `sendUntilAcknowledged does not append round ID to event that is not round-scoped`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ChatMessage, 5, arrayOf("hello"))

        val emission = transport.emissions.single()

        assertEquals(MultiplayerClientEvent.ChatMessage, emission.event)
        assertEquals(listOf<Any?>("hello"), emission.args.toList())
    }

    @Test
    fun `sendUntilAcknowledged resends on timeout while ready`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))

        transport.emissions.single().onAck(null)

        assertEquals(2, transport.emissions.size)
        assertEquals(listOf<Any?>("json", 3), transport.emissions[1].args.toList())
        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission), outbox.unacknowledged)
    }

    @Test
    fun `sendUntilAcknowledged holds event on timeout while not ready until flush`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        transport.isReady = false

        transport.emissions.single().onAck(null)

        assertEquals(1, transport.emissions.size)
        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission), outbox.unacknowledged)

        // Held items go out on the next flush.
        transport.isReady = true
        outbox.flush()

        assertEquals(2, transport.emissions.size)
    }

    @Test
    fun `flush sends held events once in order`() {
        transport.isReady = false

        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ChatMessage, null, arrayOf("hi"))

        assertTrue(transport.emissions.isEmpty())

        transport.isReady = true
        outbox.flush()
        outbox.flush()

        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission, MultiplayerClientEvent.ChatMessage), transport.emissions.map { it.event })
    }

    @Test
    fun `flush does nothing while not ready`() {
        transport.isReady = false
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))

        outbox.flush()

        assertTrue(transport.emissions.isEmpty())
    }

    @Test
    fun `acknowledgement from before disconnection is ignored`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        val first = transport.emissions.single()

        outbox.onDisconnected()
        outbox.flush()

        assertEquals(2, transport.emissions.size)
        val second = transport.emissions[1]

        first.onAck(okAckResult)
        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission), outbox.unacknowledged)

        // A stale timeout must not trigger another resend either.
        first.onAck(null)
        assertEquals(2, transport.emissions.size)

        second.onAck(okAckResult)
        assertTrue(outbox.unacknowledged.isEmpty())
    }

    @Test
    fun `timeout from before disconnection does not resend`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        val first = transport.emissions.single()

        outbox.onDisconnected()
        first.onAck(null)

        assertEquals(1, transport.emissions.size)
        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission), outbox.unacknowledged)
    }

    @Test
    fun `pruneStale drops events of other rounds and received events`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.SkipRequested, 4, emptyArray())
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ChatMessage, null, arrayOf("hi"))

        outbox.pruneStale(4, setOf(MultiplayerClientEvent.SkipRequested))

        assertEquals(listOf(MultiplayerClientEvent.ChatMessage), outbox.unacknowledged)
    }

    @Test
    fun `pruneStale keeps unreceived events of active round`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 4, arrayOf("json"))
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.SkipRequested, 4, emptyArray())

        outbox.pruneStale(4, emptySet())

        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission, MultiplayerClientEvent.SkipRequested), outbox.unacknowledged)
    }

    @Test
    fun `pruneStale without active round drops only round-scoped events`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.BeatmapLoadComplete, 3, emptyArray())
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.SkipRequested, null, emptyArray())
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ChatMessage, null, arrayOf("hi"))

        outbox.pruneStale(null, emptySet())

        assertEquals(listOf(MultiplayerClientEvent.ChatMessage), outbox.unacknowledged)
    }

    @Test
    fun `timeout of pruned event does not resend`() {
        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        val emission = transport.emissions.single()

        outbox.pruneStale(null, emptySet())
        emission.onAck(null)

        assertEquals(1, transport.emissions.size)
    }

    @Test
    fun `sendOnce reports timeout while connected as not delivered`() {
        val reports = mutableListOf<Boolean>()

        outbox.sendOnce(MultiplayerClientEvent.PlayerStatusChanged, arrayOf("x")) { reports.add(it) }

        val emission = transport.emissions.single()
        assertEquals(MultiplayerClientEvent.PlayerStatusChanged, emission.event)
        assertEquals(listOf<Any?>("x"), emission.args.toList())

        emission.onAck(null)
        emission.onAck(null)

        assertEquals(listOf(true), reports)
        // Never resent.
        assertEquals(1, transport.emissions.size)
    }

    @Test
    fun `sendOnce does not report acknowledged event`() {
        val reports = mutableListOf<Boolean>()

        outbox.sendOnce(MultiplayerClientEvent.PlayerStatusChanged, emptyArray()) { reports.add(it) }
        transport.emissions.single().onAck(okAckResult)
        outbox.onDisconnected()

        assertTrue(reports.isEmpty())
    }

    @Test
    fun `sendOnce reports disconnection as not delivered while not connected`() {
        val reports = mutableListOf<Boolean>()

        outbox.sendOnce(MultiplayerClientEvent.PlayerStatusChanged, emptyArray()) { reports.add(it) }

        outbox.onDisconnected()
        transport.emissions.single().onAck(null)

        assertEquals(listOf(false), reports)
    }

    @Test
    fun `sendOnce while not ready reports not delivered without emitting`() {
        transport.isReady = false
        val reports = mutableListOf<Boolean>()

        outbox.sendOnce(MultiplayerClientEvent.PlayerStatusChanged, emptyArray()) { reports.add(it) }

        assertEquals(listOf(false), reports)
        assertTrue(transport.emissions.isEmpty())
    }

    @Test
    fun `clear drops every event without reporting`() {
        val reports = mutableListOf<Boolean>()

        outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 3, arrayOf("json"))
        outbox.sendOnce(MultiplayerClientEvent.PlayerStatusChanged, emptyArray()) { reports.add(it) }

        outbox.clear()
        transport.emissions.forEach { it.onAck(null) }

        assertTrue(outbox.unacknowledged.isEmpty())
        assertTrue(reports.isEmpty())
        assertEquals(2, transport.emissions.size)
    }

    @Test
    fun `sendOnce not delivered callback may call back into outbox`() {
        var reentered = false

        outbox.sendOnce(MultiplayerClientEvent.PlayerStatusChanged, emptyArray()) {
            // Would deadlock or throw if the callback ran while the lock is held by another thread's critical section.
            outbox.sendUntilAcknowledged(MultiplayerClientEvent.ChatMessage, null, arrayOf("retry"))
            reentered = true
        }

        outbox.onDisconnected()

        assertTrue(reentered)
        assertEquals(listOf(MultiplayerClientEvent.ChatMessage), outbox.unacknowledged)
    }

    private class Emission(
        val event: MultiplayerClientEvent,
        val args: Array<Any?>,
        val timeoutMs: Long,
        val onAck: (MultiplayerAckResult?) -> Unit
    )

    private val transport = object : MultiplayerOutboxTransport {
        override var isReady = true
        val emissions = mutableListOf<Emission>()

        override fun emit(event: MultiplayerClientEvent, args: Array<Any?>, timeoutMs: Long, onAck: (MultiplayerAckResult?) -> Unit) {
            emissions.add(Emission(event, args, timeoutMs, onAck))
        }
    }

    private val outbox = MultiplayerOutbox(transport)

    private val okAckResult = MultiplayerAckResult(true, null)
}
