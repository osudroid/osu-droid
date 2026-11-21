package com.osudroid.multiplayer.api

/**
 * Multiplayer events that the server sends to the client.
 *
 * @property eventName The name of the [MultiplayerServerEvent].
 */
enum class MultiplayerServerEvent(val eventName: String) {
    /**
     * The server encountered an error while processing an event.
     */
    Error("error"),

    /**
     * The server accepted the connection and describes the room, the current round and the chat backlog.
     */
    InitialConnection("initialConnection"),

    /**
     * A player joined the room.
     */
    PlayerJoined("playerJoined"),

    /**
     * A player left the room.
     */
    PlayerLeft("playerLeft"),

    /**
     * A player's team was changed.
     */
    TeamChanged("teamChanged"),

    /**
     * The room's gameplay settings were changed.
     */
    RoomGameplaySettingsChanged("roomGameplaySettingsChanged"),

    /**
     * A player's status was changed.
     */
    PlayerStatusChanged("playerStatusChanged"),

    /**
     * A player's mods were changed.
     */
    PlayerModsChanged("playerModsChanged"),

    /**
     * The room's status was changed.
     */
    RoomStatusChanged("roomStatusChanged"),

    /**
     * The host started a round. Carries the ID of the round.
     */
    PlayBeatmap("playBeatmap"),

    /**
     * Every player finished loading the beatmap of the round.
     */
    AllPlayersBeatmapLoadComplete("allPlayersBeatmapLoadComplete"),

    /**
     * Every player requested to skip the intro of the round.
     */
    AllPlayersSkipRequested("allPlayersSkipRequested"),

    /**
     * Every player submitted their score. Carries the final leaderboard and the ID of the round it belongs to.
     */
    AllPlayersScoreSubmitted("allPlayersScoreSubmitted"),

    /**
     * Live score data of the players in the round.
     */
    LiveScoreData("liveScoreData"),

    /**
     * A chat message was sent. Carries the ID of the message, except for private replies to a single player.
     */
    ChatMessage("chatMessage"),

    /**
     * The room's maximum player count was changed.
     */
    MaxPlayersChanged("maxPlayersChanged"),

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
     * The room's team mode was changed.
     */
    TeamModeChanged("teamModeChanged"),

    /**
     * The room's win condition was changed.
     */
    WinConditionChanged("winConditionChanged"),

    /**
     * The room's name was changed.
     */
    RoomNameChanged("roomNameChanged"),
}
