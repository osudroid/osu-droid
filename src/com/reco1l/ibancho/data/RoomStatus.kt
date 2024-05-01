package com.reco1l.ibancho.data

enum class RoomStatus
{
    IDLE,
    CHANGING_BEATMAP,
    PLAYING;

    companion object
    {
        fun from(ordinal: Int) = values()[ordinal]
    }
}