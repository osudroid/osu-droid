package com.osudroid.discord

/**
 * Describes the user's current in-game context for Discord rich presence.
 */
sealed class UserActivity {
    /**
     * The primary line shown in Discord (e.g. beatmap title, room name). `null` omits the field.
     */
    open val details: String? = null

    /**
     * The secondary line shown in Discord (e.g. "Playing", "In a multiplayer room")
     */
    abstract val status: String

    /**
     * The current party size. `0` omits the party field from rich presence.
     */
    open val partySize = 0

    /**
     * The maximum party size.
     */
    open val partyMax = 0

    /**
     * User is idling at the main menu.
     */
    data object Idle : UserActivity() {
        override val status = "Idle"
    }

    /**
     * User is browsing song select.
     */
    data object ChoosingBeatmap : UserActivity() {
        override val status = "Choosing a beatmap"
    }

    /**
     * The player is in gameplay.
     */
    sealed class InGame(beatmapTitle: String) : UserActivity() {
        override val details = beatmapTitle
    }

    /**
     * User is playing a beatmap in single-player.
     */
    class PlayingBeatmap(beatmapTitle: String) : InGame(beatmapTitle) {
        override val status = "Playing"
    }

    /**
     * User is watching a replay.
     */
    class WatchingReplay(beatmapTitle: String) : InGame(beatmapTitle) {
        override val status = "Watching a replay"
    }

    /**
     * User is in a multiplayer room lobby.
     */
    class InMultiplayerLobby(
        roomName: String,
        override val partySize: Int,
        override val partyMax: Int
    ) : UserActivity() {
        override val status = "In a multiplayer room"
        override val details = roomName
    }

    /**
     * User is playing a beatmap in a multiplayer room.
     */
    class PlayingMultiplayer(
        beatmapTitle: String,
        override val partySize: Int,
        override val partyMax: Int,
    ) : InGame(beatmapTitle) {
        override val status = "Playing with others"
    }
}