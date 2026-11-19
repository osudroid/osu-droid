package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.MultiplayerClientEvent
import com.osudroid.multiplayer.api.data.PlayerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MultiplayerMatchSessionTest {
    @Test
    fun `phase starts in room without round`() {
        assertEquals(MultiplayerMatchPhase.InRoom(null), session.phase)
        assertNull(session.activeRoundId)
    }

    @Test
    fun `phase transitions through a full round`() {
        session.tryEnterLoading(2)
        assertEquals(MultiplayerMatchPhase.Loading(2), session.phase)
        assertEquals(2, session.activeRoundId)

        session.enterPlaying()
        assertEquals(MultiplayerMatchPhase.Playing(2), session.phase)

        session.enterResults()
        assertEquals(MultiplayerMatchPhase.Results(2, false), session.phase)

        session.enterRoom()
        assertEquals(MultiplayerMatchPhase.InRoom(2), session.phase)
    }

    @Test
    fun `enterPlaying is a no-op outside loading`() {
        session.enterPlaying()
        assertEquals(MultiplayerMatchPhase.InRoom(null), session.phase)

        session.tryEnterLoading(2)
        session.enterPlaying()
        session.enterPlaying()
        assertEquals(MultiplayerMatchPhase.Playing(2), session.phase)
    }

    @Test
    fun `enterResults is a no-op outside playing`() {
        session.enterResults()
        assertEquals(MultiplayerMatchPhase.InRoom(null), session.phase)

        session.tryEnterLoading(2)
        session.enterResults()
        assertEquals(MultiplayerMatchPhase.Loading(2), session.phase)
    }

    @Test
    fun `markLeaderboardShown is a no-op outside results without leaderboard`() {
        session.markLeaderboardShown()
        assertEquals(MultiplayerMatchPhase.InRoom(null), session.phase)

        session.tryEnterLoading(2)
        session.enterPlaying()
        session.markLeaderboardShown()
        assertEquals(MultiplayerMatchPhase.Playing(2), session.phase)

        session.enterResults()
        session.markLeaderboardShown()
        assertEquals(MultiplayerMatchPhase.Results(2, true), session.phase)

        session.markLeaderboardShown()
        assertEquals(MultiplayerMatchPhase.Results(2, true), session.phase)
    }

    @Test
    fun `tryEnterLoading enters loading from a room without round`() {
        assertTrue(session.tryEnterLoading(1))
        assertEquals(MultiplayerMatchPhase.Loading(1), session.phase)
    }

    @Test
    fun `tryEnterLoading enters loading for a new round after a sat out round`() {
        session.tryEnterLoading(1)
        session.enterRoom()
        assertEquals(MultiplayerMatchPhase.InRoom(1), session.phase)

        assertTrue(session.tryEnterLoading(2))
        assertEquals(MultiplayerMatchPhase.Loading(2), session.phase)
    }

    @Test
    fun `tryEnterLoading enters loading for a new round after results`() {
        playRoundToResults(1)
        assertEquals(MultiplayerMatchPhase.Results(1, false), session.phase)

        assertTrue(session.tryEnterLoading(2))
        assertEquals(MultiplayerMatchPhase.Loading(2), session.phase)
    }

    @Test
    fun `tryEnterLoading refuses the round that is loading`() {
        session.tryEnterLoading(2)

        assertFalse(session.tryEnterLoading(2))
        assertEquals(MultiplayerMatchPhase.Loading(2), session.phase)
    }

    @Test
    fun `tryEnterLoading refuses the round that is playing`() {
        session.tryEnterLoading(2)
        session.enterPlaying()

        assertFalse(session.tryEnterLoading(2))
        assertEquals(MultiplayerMatchPhase.Playing(2), session.phase)
    }

    @Test
    fun `tryEnterLoading refuses the round that has results`() {
        playRoundToResults(2)

        assertFalse(session.tryEnterLoading(2))
        assertEquals(MultiplayerMatchPhase.Results(2, false), session.phase)
    }

    @Test
    fun `tryEnterLoading refuses the round already sat out in the room`() {
        session.tryEnterLoading(2)
        session.enterRoom()

        assertFalse(session.tryEnterLoading(2))
        assertEquals(MultiplayerMatchPhase.InRoom(2), session.phase)
    }

    @Test
    fun `tryEnterLoading lets only one of many threads enter the same round`() {
        val threads = 16
        val start = CountDownLatch(1)
        val entered = AtomicInteger()
        val pool = Executors.newFixedThreadPool(threads)

        try {
            val done = (1..threads).map {
                pool.submit {
                    start.await()

                    if (session.tryEnterLoading(5)) {
                        entered.incrementAndGet()
                    }
                }
            }

            start.countDown()
            done.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        assertEquals(1, entered.get())
        assertEquals(MultiplayerMatchPhase.Loading(5), session.phase)
    }
    @Test
    fun `acceptsFinalLeaderboard accepts own round right after results`() {
        playRoundToResults(2)

        assertTrue(session.acceptsFinalLeaderboard(2))
    }

    @Test
    fun `acceptsFinalLeaderboard rejects another round`() {
        playRoundToResults(2)

        assertFalse(session.acceptsFinalLeaderboard(3))
    }

    @Test
    fun `acceptsFinalLeaderboard rejects after leaderboard is shown`() {
        playRoundToResults(2)
        session.markLeaderboardShown()

        assertFalse(session.acceptsFinalLeaderboard(2))
    }

    @Test
    fun `acceptsFinalLeaderboard rejects outside results`() {
        assertFalse(session.acceptsFinalLeaderboard(2))

        session.tryEnterLoading(2)
        assertFalse(session.acceptsFinalLeaderboard(2))

        session.enterPlaying()
        assertFalse(session.acceptsFinalLeaderboard(2))

        session.enterResults()
        session.enterRoom()
        assertFalse(session.acceptsFinalLeaderboard(2))
    }

    @Test
    fun `acceptChat accepts first id when unseeded`() {
        assertNull(session.lastChatId)
        assertTrue(session.acceptChat(5))
        assertEquals(5, session.lastChatId)
    }

    @Test
    fun `acceptChat drops equal and lower ids`() {
        assertTrue(session.acceptChat(5))

        assertFalse(session.acceptChat(5))
        assertFalse(session.acceptChat(4))
        assertTrue(session.acceptChat(6))
        assertEquals(6, session.lastChatId)
    }

    @Test
    fun `seedChat makes acceptChat drop seeded id and accept the next`() {
        session.seedChat(10)

        assertFalse(session.acceptChat(10))
        assertTrue(session.acceptChat(11))
    }

    @Test
    fun `recover flushes before returning`() {
        transport.isReady = false
        session.enterRoom()
        session.tryEnterLoading(2)
        session.enterPlaying()
        session.enterResults()
        session.enterRoom()

        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 2, arrayOf("json"))
        transport.isReady = true

        val snapshot = MultiplayerRecoverySnapshot(
            roundId = 2,
            round = MultiplayerRoundProgress(
                allLoaded = true,
                allSkipped = false,
                loadReceived = true,
                skipReceived = false,
                scoreReceived = false
            ),
            lastResultsRoundId = null,
            clientStatus = PlayerStatus.Playing
        )

        val actions = session.recover(snapshot, hasBeatmap = true)

        // The resent score is on the wire before the caller gets a chance to emit anything else.
        assertEquals(listOf(MultiplayerClientEvent.ScoreSubmission), transport.emissions.map { it.event })
        assertEquals(listOf<Any?>("json", 2), transport.emissions.single().args.toList())

        assertEquals(reconcile(MultiplayerMatchPhase.InRoom(2), snapshot, true), actions)
        assertEquals(listOf(MultiplayerReconcileAction.InvalidateStatus), actions)
    }

    @Test
    fun `recover does not change phase`() {
        session.tryEnterLoading(2)

        session.recover(MultiplayerRecoverySnapshot(3, null, null, PlayerStatus.NotReady), hasBeatmap = true)

        assertEquals(MultiplayerMatchPhase.Loading(2), session.phase)
    }

    @Test
    fun `recover drops stale score`() {
        transport.isReady = false
        playRoundToResults(2)

        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 2, arrayOf("json"))
        transport.isReady = true

        val actions = session.recover(
            MultiplayerRecoverySnapshot(roundId = 2, round = null, lastResultsRoundId = 2, clientStatus = PlayerStatus.Ready),
            hasBeatmap = true
        )

        assertTrue(transport.emissions.none { it.event == MultiplayerClientEvent.ScoreSubmission })
        assertTrue(session.outbox.unacknowledged.isEmpty())
        assertEquals(
            listOf(MultiplayerReconcileAction.ShowLastResults, MultiplayerReconcileAction.ReleaseStatusLock),
            actions
        )
    }

    @Test
    fun `recover drops events the server already received`() {
        transport.isReady = false
        session.tryEnterLoading(2)
        session.enterPlaying()

        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.BeatmapLoadComplete, 2, emptyArray())
        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.SkipRequested, 2, emptyArray())
        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 2, arrayOf("json"))
        transport.isReady = true

        session.recover(
            MultiplayerRecoverySnapshot(
                roundId = 2,
                round = MultiplayerRoundProgress(
                    allLoaded = true,
                    allSkipped = false,
                    loadReceived = true,
                    skipReceived = false,
                    scoreReceived = true
                ),
                lastResultsRoundId = null,
                clientStatus = PlayerStatus.Playing
            ),
            hasBeatmap = true
        )

        // Only the skip request the server has not received is resent.
        assertEquals(listOf(MultiplayerClientEvent.SkipRequested), transport.emissions.map { it.event })
        assertEquals(listOf(MultiplayerClientEvent.SkipRequested), session.outbox.unacknowledged)
    }

    @Test
    fun `recover drops round-scoped events of another round`() {
        transport.isReady = false
        session.tryEnterLoading(1)
        session.enterPlaying()

        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 1, arrayOf("json"))
        transport.isReady = true

        session.recover(
            MultiplayerRecoverySnapshot(
                roundId = 2,
                round = MultiplayerRoundProgress(
                    allLoaded = false,
                    allSkipped = false,
                    loadReceived = false,
                    skipReceived = false,
                    scoreReceived = false
                ),
                lastResultsRoundId = null,
                clientStatus = PlayerStatus.Playing
            ),
            hasBeatmap = true
        )

        assertTrue(transport.emissions.isEmpty())
        assertTrue(session.outbox.unacknowledged.isEmpty())
    }

    @Test
    fun `recover keeps events that are not round-scoped without an active round`() {
        transport.isReady = false
        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ChatMessage, null, arrayOf("hi"))
        transport.isReady = true

        session.recover(MultiplayerRecoverySnapshot(2, null, null, PlayerStatus.NotReady), hasBeatmap = true)

        assertEquals(listOf(MultiplayerClientEvent.ChatMessage), transport.emissions.map { it.event })
    }

    @Test
    fun `reset clears phase chat and outbox`() {
        transport.isReady = false
        playRoundToResults(2)
        session.seedChat(7)
        session.outbox.sendUntilAcknowledged(MultiplayerClientEvent.ScoreSubmission, 2, arrayOf("json"))

        session.reset()

        assertEquals(MultiplayerMatchPhase.InRoom(null), session.phase)
        assertNull(session.lastChatId)
        assertTrue(session.outbox.unacknowledged.isEmpty())
    }

    private fun playRoundToResults(roundId: Int) {
        session.tryEnterLoading(roundId)
        session.enterPlaying()
        session.enterResults()
    }

    private class Emission(val event: MultiplayerClientEvent, val args: Array<Any?>)

    private val transport = object : MultiplayerOutboxTransport {
        override var isReady = true
        val emissions = mutableListOf<Emission>()

        override fun emit(event: MultiplayerClientEvent, args: Array<Any?>, timeoutMs: Long, onAck: OnMultiplayerEventAck) {
            emissions.add(Emission(event, args))
        }
    }

    private val session = MultiplayerMatchSession(MultiplayerOutbox(transport))
}
