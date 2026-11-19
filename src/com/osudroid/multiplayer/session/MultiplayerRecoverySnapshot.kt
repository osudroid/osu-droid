package com.osudroid.multiplayer.session

import com.osudroid.multiplayer.api.data.PlayerStatus

/**
 * The progress of the current round, as reported by the server on reconnection.
 *
 * @property allLoaded Whether every participating player has finished loading.
 * @property allSkipped Whether every participating player has skipped the intro.
 * @property loadReceived Whether the server has received this client's load completion.
 * @property skipReceived Whether the server has received this client's skip request.
 * @property scoreReceived Whether the server has received this client's final score.
 */
data class MultiplayerRoundProgress(
    val allLoaded: Boolean,
    val allSkipped: Boolean,
    val loadReceived: Boolean,
    val skipReceived: Boolean,
    val scoreReceived: Boolean
)

/**
 * The state the server reports to a reconnecting client.
 *
 * @property roundId The ID of the current round.
 * @property round The progress of the current round. This is non-null if and only if the room is playing.
 * @property lastResultsRoundId The ID of the last round whose results are available, if any.
 * @property ownStatus The server-side status of this client, if known.
 */
data class MultiplayerRecoverySnapshot(
    val roundId: Int,
    val round: MultiplayerRoundProgress?,
    val lastResultsRoundId: Int?,
    val ownStatus: PlayerStatus?
)
