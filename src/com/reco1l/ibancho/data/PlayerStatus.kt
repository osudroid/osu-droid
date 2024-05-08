package com.reco1l.ibancho.data

enum class PlayerStatus
{
    NOT_READY,
    READY,
    MISSING_BEATMAP,
    PLAYING;

    companion object
    {
        fun from(ordinal: Int) = entries[ordinal]
    }
}