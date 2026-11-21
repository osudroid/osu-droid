package com.osudroid.multiplayer.api

/**
 * Multiplayer events that the client sends to the server.
 *
 * @property eventName The name of the [MultiplayerClientEvent].
 * @property isRoundScoped Whether this [MultiplayerClientEvent] is scoped to a round. When the
 * current round changes, a round-scoped [MultiplayerClientEvent] is considered stale.
 */
enum class MultiplayerClientEvent(val eventName: String, val isRoundScoped: Boolean = false) {
    /**
     * The room's beatmap was changed.
     */
    BeatmapChanged("beatmapChanged"),

    /**
     * The room's host was changed.
     */
    HostChanged("hostChanged"),

    /**
     * A player was kicked from the room.
     */
    PlayerKicked("playerKicked"),

    /**
     * The room's mods were changed.
     */
    RoomModsChanged("roomModsChanged"),

    /**
     * The local player's mods were changed.
     */
    PlayerModsChanged("playerModsChanged"),

    /**
     * The room's gameplay settings were changed.
     */
    RoomGameplaySettingsChanged("roomGameplaySettingsChanged"),

    /**
     * The local player's status was changed.
     */
    PlayerStatusChanged("playerStatusChanged"),

    /**
     * The room's team mode was changed.
     */
    TeamModeChanged("teamModeChanged"),

    /**
     * The local player's team was changed.
     */
    TeamChanged("teamChanged"),

    /**
     * The room's win condition was changed.
     */
    WinConditionChanged("winConditionChanged"),

    /**
     * The room's name was changed.
     */
    RoomNameChanged("roomNameChanged"),

    /**
     * The room's password was changed.
     */
    RoomPasswordChanged("roomPasswordChanged"),

    /**
     * The room's maximum player count was changed.
     */
    MaxPlayersChanged("maxPlayersChanged"),

    /**
     * The host requested to start the round.
     */
    PlayBeatmap("playBeatmap"),

    /**
     * A chat message was sent.
     */
    ChatMessage("chatMessage"),

    /**
     * The local player finished loading the beatmap.
     */
    BeatmapLoadComplete("beatmapLoadComplete", isRoundScoped = true),

    /**
     * The local player requested to skip the intro.
     */
    SkipRequested("skipRequested", isRoundScoped = true),

    /**
     * The local player's score at the end of a round was submitted.
     */
    ScoreSubmission("scoreSubmission", isRoundScoped = true),

    /**
     * The local player's live score data was sent.
     */
    LiveScoreData("liveScoreData"),
}
