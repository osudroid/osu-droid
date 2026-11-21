package com.osudroid.multiplayer.api.data

/**
 * A public chat message in a multiplayer room.
 */
sealed interface MultiplayerMessage {
    /**
     * The ID of this [MultiplayerMessage].
     */
    val id: Int

    /**
     * The content of this [MultiplayerMessage].
     */
    val message: String

    /**
     * A [MultiplayerMessage] sent by a player.
     *
     * @property uid The user ID of the player who sent this [MultiplayerMessage].
     */
    data class Player(override val id: Int, val uid: Long, override val message: String) : MultiplayerMessage

    /**
     * A [MultiplayerMessage] sent by the server.
     */
    data class System(override val id: Int, override val message: String) : MultiplayerMessage
}
