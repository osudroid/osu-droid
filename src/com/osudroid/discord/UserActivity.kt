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
     * User is searching beatmaps in beatmap listing.
     */
    data object SearchingBeatmap : UserActivity() {
        override val status = "Searching for beatmaps"
    }

    /**
     * The player is in gameplay.
     *
     * @param beatmapId The beatmap ID.
     */
    sealed class InGame(beatmapTitle: String, beatmapId: Long?) : UserActivity() {
        val beatmapUrl = if (beatmapId != null && beatmapId != -1L) "https://osu.ppy.sh/b/$beatmapId" else null

        override val details = beatmapTitle
    }

    /**
     * User is playing a beatmap in single-player.
     */
    class InSoloGame(beatmapTitle: String, beatmapId: Long?) : InGame(beatmapTitle, beatmapId) {
        override val status = "Playing"
    }

    /**
     * User is watching a replay.
     */
    class WatchingReplay(playerName: String, beatmapTitle: String, beatmapId: Long?) : InGame(beatmapTitle, beatmapId) {
        override val status = "Watching $playerName's replay"
    }

    /**
     * User is searching for a multiplayer lobby.
     */
    data object SearchingForMultiplayerLobby : UserActivity() {
        override val status = "Looking for a multiplayer lobby"
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
    class InMultiplayerGame(
        beatmapTitle: String,
        beatmapId: Long?,
        override val partySize: Int,
        override val partyMax: Int,
    ) : InGame(beatmapTitle, beatmapId) {
        override val status = "Playing with others"
    }
}