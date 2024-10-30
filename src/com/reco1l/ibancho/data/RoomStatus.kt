package com.reco1l.ibancho.data

/**
 * Represents the status of a room.
 */
enum class RoomStatus {

    /**
     * The room is idle (not playing).
     */
    Idle,

    /**
     * The room is in the process of changing the beatmap.
     */
    ChangingBeatmap,

    /**
     * The room is playing.
     */
    Playing;


    companion object {
        operator fun get(ordinal: Int) = entries[ordinal]
    }
}