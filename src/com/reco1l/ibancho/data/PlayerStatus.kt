package com.reco1l.ibancho.data

/**
 * Represents the status of a player in a multiplayer room.
 */
enum class PlayerStatus {

    /**
     * The player is not ready.
     */
    NOT_READY,

    /**
     * The player is ready.
     */
    READY,

    /**
     * The player does not have the selected beatmap.
     */
    MISSING_BEATMAP,

    /**
     * The player is playing... Duh
     */
    PLAYING;


    companion object {
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}