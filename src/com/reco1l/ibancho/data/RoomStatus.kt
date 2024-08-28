package com.reco1l.ibancho.data

/**
 * Represents the status of a room.
 */
enum class RoomStatus {

    /**
     * The room is idle (not playing).
     */
    IDLE,

    /**
     * The room is in the process of changing the beatmap.
     */
    CHANGING_BEATMAP,

    /**
     * The room is playing.
     */
    PLAYING;


    companion object {
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}