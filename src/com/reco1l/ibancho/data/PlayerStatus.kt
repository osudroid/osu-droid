package com.reco1l.ibancho.data

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
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}