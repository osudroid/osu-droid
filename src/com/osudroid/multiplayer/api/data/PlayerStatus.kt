package com.osudroid.multiplayer.api.data

/**
 * Represents the status of a player in a multiplayer room.
 */
enum class PlayerStatus {

    /**
     * The player is not ready.
     */
    NotReady,

    /**
     * The player is ready.
     */
    Ready,

    /**
     * The player does not have the selected beatmap.
     */
    MissingBeatmap,

    /**
     * The player is playing... Duh
     */
    Playing;


    companion object {
        /**
         * Returns the [PlayerStatus] for the given wire string, or `null` if not recognized.
         */
        fun fromWire(value: String): PlayerStatus? = runCatching { enumValueOf<PlayerStatus>(value) }.getOrNull()
    }
}
