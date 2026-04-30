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
         * Returns the [PlayerStatus] for [ordinal], or `null` if the ordinal is not recognised
         * (e.g. the server added a new status that this client does not know about).
         * Callers must handle the `null` case instead of crashing with AIOOBE (EH-1).
         */
        operator fun get(ordinal: Int): PlayerStatus? = entries.getOrNull(ordinal)
    }
}