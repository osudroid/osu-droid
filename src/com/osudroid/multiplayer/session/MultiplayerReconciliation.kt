package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.data.PlayerStatus

/**
 * An action the client should perform to converge with the server after a reconnection.
 */
sealed interface MultiplayerReconcileAction {
    /**
     * Discard the locally cached player status and re-sync it with the server.
     */
    data object InvalidateStatus : MultiplayerReconcileAction

    /**
     * Release the lock that prevents status changes while reconciling.
     */
    data object ReleaseStatusLock : MultiplayerReconcileAction

    /**
     * Show the results of the last finished round.
     */
    data object ShowLastResults : MultiplayerReconcileAction

    /**
     * Start the game immediately, since every player has already loaded.
     */
    data object StartImmediately : MultiplayerReconcileAction

    /**
     * Apply the intro skip, since every player has already skipped.
     */
    data object ApplySkip : MultiplayerReconcileAction

    /**
     * Join the given round that is still loading.
     */
    data class JoinRound(val roundId: Int) : MultiplayerReconcileAction

    /**
     * Sit out the current round.
     */
    data object SitOut : MultiplayerReconcileAction
}

/**
 * Decides what the client should do after a reconnection.
 *
 * @param phase The phase the client was in when it reconnected.
 * @param snapshot The state reported by the server.
 * @param hasBeatmap Whether the client has the current beatmap.
 * @return The actions to perform, in order.
 */
internal fun reconcile(phase: MultiplayerMatchPhase, snapshot: MultiplayerRecoverySnapshot, hasBeatmap: Boolean): List<MultiplayerReconcileAction> {
    val round = snapshot.round
    val phaseRoundId = phase.roundId

    // Previous results are only shown if the client is on a results screen without a leaderboard that matches the
    // last results the server holds.
    val showLastResults = phase is MultiplayerMatchPhase.Results &&
        !phase.hasLeaderboard &&
        snapshot.lastResultsRoundId == phase.roundId

    fun withLastResults(vararg actions: MultiplayerReconcileAction) =
        if (showLastResults) listOf(MultiplayerReconcileAction.ShowLastResults, *actions) else actions.toList()

    if (round == null) {
        return when (phase) {
            is MultiplayerMatchPhase.InRoom -> listOf(MultiplayerReconcileAction.InvalidateStatus)
            else -> withLastResults(MultiplayerReconcileAction.ReleaseStatusLock)
        }
    }

    if (snapshot.roundId == phaseRoundId) {
        return when (phase) {
            is MultiplayerMatchPhase.InRoom -> listOf(MultiplayerReconcileAction.InvalidateStatus)
            is MultiplayerMatchPhase.Results -> listOf(MultiplayerReconcileAction.ReleaseStatusLock)

            is MultiplayerMatchPhase.Loading ->
                if (round.allLoaded) listOf(MultiplayerReconcileAction.StartImmediately, MultiplayerReconcileAction.ReleaseStatusLock)
                else listOf(MultiplayerReconcileAction.ReleaseStatusLock)

            is MultiplayerMatchPhase.Playing ->
                if (round.allSkipped) listOf(MultiplayerReconcileAction.ApplySkip, MultiplayerReconcileAction.ReleaseStatusLock)
                else listOf(MultiplayerReconcileAction.ReleaseStatusLock)
        }
    }

    if (snapshot.clientStatus == PlayerStatus.Playing) {
        return when (phase) {
            is MultiplayerMatchPhase.InRoom, is MultiplayerMatchPhase.Results ->
                if (!round.allLoaded && hasBeatmap) withLastResults(MultiplayerReconcileAction.JoinRound(snapshot.roundId))
                else withLastResults(MultiplayerReconcileAction.SitOut)

            // Never restart a running game.
            is MultiplayerMatchPhase.Loading, is MultiplayerMatchPhase.Playing -> listOf(MultiplayerReconcileAction.SitOut)
        }
    }

    return when (phase) {
        is MultiplayerMatchPhase.InRoom -> listOf(MultiplayerReconcileAction.InvalidateStatus)
        else -> withLastResults(MultiplayerReconcileAction.ReleaseStatusLock)
    }
}
