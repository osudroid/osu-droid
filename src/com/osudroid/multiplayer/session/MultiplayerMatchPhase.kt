package com.osudroid.multiplayer.session

/**
 * The phase of a multiplayer match as seen by the local client.
 */
sealed interface MultiplayerMatchPhase {
    /**
     * The client is in the room lobby.
     *
     * @property lastRoundId The ID of the last round the client took part in or observed, if any.
     */
    data class InRoom(val lastRoundId: Int?) : MultiplayerMatchPhase

    /**
     * The client is loading the beatmap of the given round.
     */
    data class Loading(val roundId: Int) : MultiplayerMatchPhase

    /**
     * The client is playing the given round.
     */
    data class Playing(val roundId: Int) : MultiplayerMatchPhase

    /**
     * The client is on the results screen of the given round.
     *
     * @property hasLeaderboard Whether the results screen already shows the room leaderboard.
     */
    data class Results(val roundId: Int, val hasLeaderboard: Boolean) : MultiplayerMatchPhase
}

/**
 * The round ID associated with this [MultiplayerMatchPhase].
 */
val MultiplayerMatchPhase.roundId: Int?
    get() = when (this) {
        is MultiplayerMatchPhase.InRoom -> lastRoundId
        is MultiplayerMatchPhase.Loading -> roundId
        is MultiplayerMatchPhase.Playing -> roundId
        is MultiplayerMatchPhase.Results -> roundId
    }
