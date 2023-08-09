package com.reco1l.api.ibancho.data

enum class PlayerStatus
{
    NOT_READY,
    READY,
    MISSING_BEATMAP,
    PLAYING;

    companion object
    {
        fun from(ordinal: Int) = values()[ordinal]
    }
}