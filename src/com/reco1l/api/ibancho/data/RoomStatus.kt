package com.reco1l.api.ibancho.data

enum class RoomStatus
{
    IDLE,
    CHANGING_BEATMAP,
    PLAYING;

    companion object
    {
        fun from(ordinal: Int) = entries[ordinal]
    }
}