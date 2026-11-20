package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.session.MultiplayerReconcileAction.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiplayerReconciliationTest {
    @Test
    fun `room without an active round invalidates status`() {
        assertEquals(
            listOf(InvalidateStatus),
            reconcile(MultiplayerMatchPhase.InRoom(3), notPlaying(), true)
        )
    }

    @Test
    fun `results without leaderboard and without an active round show last results only if they match`() {
        assertEquals(
            listOf(ShowLastResults, ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Results(4, false), notPlaying(lastResultsRoundId = 4), true)
        )
        assertEquals(
            listOf(ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Results(4, false), notPlaying(lastResultsRoundId = 3), true)
        )
        assertEquals(
            listOf(ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Results(4, false), notPlaying(), true)
        )
    }

    @Test
    fun `results with leaderboard or loading or playing without an active round release status lock`() {
        val snapshot = notPlaying(lastResultsRoundId = 4)

        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Results(4, true), snapshot, true))
        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Loading(4), snapshot, true))
        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Playing(4), snapshot, true))
    }

    @Test
    fun `room that sat out the active round invalidates status`() {
        assertEquals(
            listOf(InvalidateStatus),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(4), true)
        )
    }

    @Test
    fun `results of the active round release status lock without joining it`() {
        val actions = reconcile(MultiplayerMatchPhase.Results(4, false), playing(4, progress()), true)

        assertEquals(listOf(ReleaseStatusLock), actions)
        assertTrue(actions.none { it is JoinRound })

        assertEquals(
            listOf(ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Results(4, true), playing(4), true)
        )
    }

    @Test
    fun `loading the active round starts immediately only if every player loaded`() {
        assertEquals(
            listOf(StartImmediately, ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Loading(4), playing(4, progress(allLoaded = true)), true)
        )
        assertEquals(
            listOf(ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Loading(4), playing(4, progress()), true)
        )
    }

    @Test
    fun `playing the active round applies skip only if every player skipped`() {
        assertEquals(
            listOf(ApplySkip, ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Playing(4), playing(4, progress(allSkipped = true)), true)
        )
        assertEquals(
            listOf(ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Playing(4), playing(4, progress()), true)
        )
    }

    @Test
    fun `room joins a new round that is still loading`() {
        assertEquals(
            listOf(JoinRound(5)),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, progress()), true)
        )
    }

    @Test
    fun `room sits out a new round every player already loaded`() {
        assertEquals(
            listOf(SitOut),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, progress(allLoaded = true)), true)
        )
    }

    @Test
    fun `room sits out a new round without the beatmap`() {
        assertEquals(
            listOf(SitOut),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, progress()), false)
        )
    }

    @Test
    fun `results show last results only without leaderboard before joining or sitting out a new round`() {
        assertEquals(
            listOf(ShowLastResults, JoinRound(5)),
            reconcile(
                MultiplayerMatchPhase.Results(4, false),
                playing(5, progress(), lastResultsRoundId = 4),
                true
            )
        )
        assertEquals(
            listOf(JoinRound(5)),
            reconcile(
                MultiplayerMatchPhase.Results(4, true),
                playing(5, progress(), lastResultsRoundId = 4),
                true
            )
        )
        assertEquals(
            listOf(ShowLastResults, SitOut),
            reconcile(
                MultiplayerMatchPhase.Results(4, false),
                playing(5, progress(allLoaded = true), lastResultsRoundId = 4),
                true
            )
        )
    }

    @Test
    fun `results join a new round without showing last results of another round`() {
        assertEquals(
            listOf(JoinRound(5)),
            reconcile(
                MultiplayerMatchPhase.Results(4, false),
                playing(5, progress(), lastResultsRoundId = 3),
                true
            )
        )
    }

    @Test
    fun `results with leaderboard sit out a new round every player already loaded`() {
        assertEquals(
            listOf(SitOut),
            reconcile(
                MultiplayerMatchPhase.Results(4, true),
                playing(5, progress(allLoaded = true), lastResultsRoundId = 4),
                true
            )
        )
    }

    @Test
    fun `results without leaderboard show last results and sit out a new round without the beatmap`() {
        assertEquals(
            listOf(ShowLastResults, SitOut),
            reconcile(
                MultiplayerMatchPhase.Results(4, false),
                playing(5, progress(), lastResultsRoundId = 4),
                false
            )
        )
    }

    @Test
    fun `loading or playing sits out a new round`() {
        val snapshot = playing(5, progress())

        assertEquals(listOf(SitOut), reconcile(MultiplayerMatchPhase.Loading(4), snapshot, true))
        assertEquals(listOf(SitOut), reconcile(MultiplayerMatchPhase.Playing(4), snapshot, true))
    }

    @Test
    fun `room invalidates status if not playing the new round`() {
        assertEquals(
            listOf(InvalidateStatus),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, clientStatus = PlayerStatus.Ready), true)
        )
    }

    @Test
    fun `results or loading or playing release status lock if not playing the new round`() {
        val snapshot = playing(5, clientStatus = PlayerStatus.Ready, lastResultsRoundId = 4)

        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Results(4, true), snapshot, true))
        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Loading(4), snapshot, true))
        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Playing(4), snapshot, true))
        assertEquals(
            listOf(ShowLastResults, ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Results(4, false), snapshot, true)
        )
    }

    private fun progress(
        allLoaded: Boolean = false,
        allSkipped: Boolean = false,
        loadReceived: Boolean = false,
        skipReceived: Boolean = false,
        scoreReceived: Boolean = false
    ) = MultiplayerRoundProgress(allLoaded, allSkipped, loadReceived, skipReceived, scoreReceived)

    private fun notPlaying(
        roundId: Int = 4,
        lastResultsRoundId: Int? = null,
        clientStatus: PlayerStatus = PlayerStatus.NotReady
    ) = MultiplayerRecoverySnapshot(roundId, null, lastResultsRoundId, clientStatus)

    private fun playing(
        roundId: Int,
        round: MultiplayerRoundProgress = progress(),
        lastResultsRoundId: Int? = null,
        clientStatus: PlayerStatus = PlayerStatus.Playing
    ) = MultiplayerRecoverySnapshot(roundId, round, lastResultsRoundId, clientStatus)
}
