package com.reco1l.api.ibancho.data

enum class RoomTeam
{
    RED,
    BLUE;

    companion object
    {
        fun from(ordinal: Int) = values()[ordinal]
    }
}