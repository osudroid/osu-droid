package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.session.MultiplayerReconcileAction.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiplayerReconciliationTest {
    @Test
    fun notPlaying_room_invalidatesStatus() {
        assertEquals(
            listOf(InvalidateStatus),
            reconcile(MultiplayerMatchPhase.InRoom(3), notPlaying(), true)
        )
    }

    @Test
    fun notPlaying_resultsWithoutLeaderboard_showsLastResultsOnlyIfMatching() {
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
    fun notPlaying_resultsWithLeaderboardLoadingPlaying_releasesStatusLock() {
        val snapshot = notPlaying(lastResultsRoundId = 4)

        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Results(4, true), snapshot, true))
        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Loading(4), snapshot, true))
        assertEquals(listOf(ReleaseStatusLock), reconcile(MultiplayerMatchPhase.Playing(4), snapshot, true))
    }

    @Test
    fun playingSameRound_room_invalidatesStatus() {
        assertEquals(
            listOf(InvalidateStatus),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(4), true)
        )
    }

    @Test
    fun playingSameRound_results_doesNotStartGame() {
        val actions = reconcile(MultiplayerMatchPhase.Results(4, false), playing(4, progress()), true)

        assertEquals(listOf(ReleaseStatusLock), actions)
        assertTrue(actions.none { it is JoinRound })

        assertEquals(
            listOf(ReleaseStatusLock),
            reconcile(MultiplayerMatchPhase.Results(4, true), playing(4), true)
        )
    }

    @Test
    fun playingSameRound_loading_startsImmediatelyOnlyIfAllLoaded() {
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
    fun playingSameRound_playing_appliesSkipOnlyIfAllSkipped() {
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
    fun playingNewRound_room_stillLoading_joins() {
        assertEquals(
            listOf(JoinRound(5)),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, progress()), true)
        )
    }

    @Test
    fun playingNewRound_room_alreadyLoaded_sitsOut() {
        assertEquals(
            listOf(SitOut),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, progress(allLoaded = true)), true)
        )
    }

    @Test
    fun playingNewRound_room_missingBeatmap_sitsOut() {
        assertEquals(
            listOf(SitOut),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, progress()), false)
        )
    }

    @Test
    fun playingNewRound_results_showsPreviousResultsThenJoins() {
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
    fun playingNewRound_resultsWithMismatchedLastResults_joinsWithoutShowingThem() {
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
    fun playingNewRound_resultsWithLeaderboard_alreadyLoaded_sitsOut() {
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
    fun playingNewRound_results_missingBeatmap_showsPreviousResultsThenSitsOut() {
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
    fun playingNewRound_loadingOrPlaying_sitsOut() {
        val snapshot = playing(5, progress())

        assertEquals(listOf(SitOut), reconcile(MultiplayerMatchPhase.Loading(4), snapshot, true))
        assertEquals(listOf(SitOut), reconcile(MultiplayerMatchPhase.Playing(4), snapshot, true))
    }

    @Test
    fun playingNewRound_ownNotPlaying_room_invalidatesStatus() {
        assertEquals(
            listOf(InvalidateStatus),
            reconcile(MultiplayerMatchPhase.InRoom(4), playing(5, clientStatus = PlayerStatus.Ready), true)
        )
    }

    @Test
    fun playingNewRound_ownNotPlaying_other_releasesStatusLock() {
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
