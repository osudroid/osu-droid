package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.MultiplayerClientEvent
import com.osudroid.multiplayer.api.data.MultiplayerMessage

/**
 * Tracks the state of the multiplayer match the client is in, and decides how to recover after a reconnection.
 *
 * This class is thread-safe.
 *
 * @property outbox The [MultiplayerOutbox] that holds the events to deliver to the server.
 */
class MultiplayerMatchSession(val outbox: MultiplayerOutbox) {
    private val lock = Any()

    /**
     * The current [MultiplayerMatchPhase].
     */
    @Volatile
    var phase: MultiplayerMatchPhase = MultiplayerMatchPhase.InRoom(null)
        private set

    /**
     * The ID of the round the match is currently in, if any.
     */
    val activeRoundId: Int?
        get() = phase.roundId

    /**
     * The ID of the latest chat message that was accepted, or `null` if it has not been seeded yet.
     */
    @Volatile
    var lastChatId: Int? = null
        private set

    /**
     * Enters the loading phase of the given round, unless that round was already started or already sat out.
     *
     * This method is thread-safe.
     *
     * @param roundId The ID of the round.
     * @return `true` if the session is now loading the round, `false` if the round was already started or
     * sat out and nothing changed.
     */
    fun tryEnterLoading(roundId: Int): Boolean {
        synchronized(lock) {
            if (roundId == phase.roundId) {
                return false
            }

            phase = MultiplayerMatchPhase.Loading(roundId)
            return true
        }
    }

    /**
     * Enters the playing phase. This does nothing unless the session is loading a round.
     */
    fun enterPlaying() {
        synchronized(lock) {
            val current = phase

            if (current is MultiplayerMatchPhase.Loading) {
                phase = MultiplayerMatchPhase.Playing(current.roundId)
            }
        }
    }

    /**
     * Enters the results phase. This does nothing unless the session is playing a round.
     */
    fun enterResults() {
        synchronized(lock) {
            val current = phase

            if (current is MultiplayerMatchPhase.Playing) {
                phase = MultiplayerMatchPhase.Results(current.roundId, false)
            }
        }
    }

    /**
     * Returns to the room lobby, remembering the round that was last played.
     */
    fun enterRoom() {
        synchronized(lock) {
            phase = MultiplayerMatchPhase.InRoom(phase.roundId)
        }
    }

    /**
     * Returns to the room lobby, leaving the round that is being played, if any.
     *
     * This method is thread-safe.
     *
     * @return `true` if a round was being played, so its score should be submitted.
     */
    fun leaveRound(): Boolean {
        synchronized(lock) {
            val wasPlaying = phase is MultiplayerMatchPhase.Playing
            phase = MultiplayerMatchPhase.InRoom(phase.roundId)

            return wasPlaying
        }
    }

    /**
     * Whether the room leaderboard sent by the server should be shown on the results.
     *
     * @param roundId The ID of the round the leaderboard belongs to.
     */
    fun acceptsFinalLeaderboard(roundId: Int): Boolean {
        val current = phase

        return current is MultiplayerMatchPhase.Results && !current.hasLeaderboard && roundId == current.roundId
    }

    /**
     * Marks that the results show the room leaderboard. This does nothing unless the results do not show it yet.
     */
    fun markLeaderboardShown() {
        synchronized(lock) {
            val current = phase

            if (current is MultiplayerMatchPhase.Results && !current.hasLeaderboard) {
                phase = MultiplayerMatchPhase.Results(current.roundId, true)
            }
        }
    }

    /**
     * Sets the ID of the latest chat message the client already has.
     *
     * @param latestChatId The ID of the latest chat message.
     */
    fun seedChat(latestChatId: Int) {
        synchronized(lock) {
            lastChatId = latestChatId
        }
    }

    /**
     * Records a chat message and tells whether it is new.
     *
     * @param id The ID of the chat message.
     * @return `true` if the message was not seen before and should be shown, `false` if it is a duplicate.
     */
    fun acceptChat(id: Int): Boolean {
        synchronized(lock) {
            val last = lastChatId

            if (last != null && id <= last) {
                return false
            }

            lastChatId = id
            return true
        }
    }

    /**
     * Prepares the session for a newly established connection to the room.
     *
     * On a fresh connection, everything about the previous match is forgotten, as [reset] does, and every chat
     * message that already exists in the room is marked as seen, so none of them is returned.
     *
     * On a reconnection, [phase] and [outbox] are left untouched, and the chat messages that were missed while
     * disconnected are recorded as seen and returned. Messages that were already seen, including duplicates within
     * the recent chat, are dropped.
     *
     * @param recovery The recovery data reported by the server.
     * @param isReconnection Whether the connection continues a connection to the room the client was already in, as
     * opposed to joining the room anew.
     * @return The missed chat messages to show, oldest first. This is always empty on a fresh connection.
     */
    fun connect(recovery: MultiplayerRecoveryPayload, isReconnection: Boolean): List<MultiplayerMessage> {
        if (!isReconnection) {
            reset()
            seedChat(recovery.latestChatId)

            return emptyList()
        }

        return recovery.recentChat.filter { acceptChat(it.id) }
    }

    /**
     * Reconciles the client with the state the server reported after a reconnection.
     *
     * Events in [outbox] that are no longer relevant are dropped and the remaining ones are sent before this
     * method returns, so they reach the server ahead of anything the caller emits while applying the returned
     * actions.
     *
     * [phase] is not changed. Applying the actions is up to the caller.
     *
     * @param snapshot The state reported by the server.
     * @param hasBeatmap Whether the client has the current beatmap.
     * @return The actions to perform, in order.
     */
    fun recover(snapshot: MultiplayerRecoverySnapshot, hasBeatmap: Boolean): List<MultiplayerReconcileAction> {
        val phaseSnapshot = phase
        val actions = reconcile(phaseSnapshot, snapshot, hasBeatmap)

        val round = snapshot.round
        val received = mutableSetOf<MultiplayerClientEvent>()

        if (round != null) {
            if (round.loadReceived) {
                received.add(MultiplayerClientEvent.BeatmapLoadComplete)
            }

            if (round.skipReceived) {
                received.add(MultiplayerClientEvent.SkipRequested)
            }

            if (round.scoreReceived) {
                received.add(MultiplayerClientEvent.ScoreSubmission)
            }
        }

        // The outbox is called without the lock held so that its callbacks can never deadlock against this session.
        outbox.pruneStale(round?.let { snapshot.roundId }, received)
        outbox.flush()

        return actions
    }

    /**
     * Forgets everything about the current match, including events waiting to be delivered.
     */
    fun reset() {
        synchronized(lock) {
            phase = MultiplayerMatchPhase.InRoom(null)
            lastChatId = null
        }

        outbox.clear()
    }
}
