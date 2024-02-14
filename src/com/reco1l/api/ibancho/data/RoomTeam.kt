package com.reco1l.api.ibancho.data

enum class RoomTeam
{
    RED,
    BLUE;


    override fun toString() = when(this)
    {
        RED -> "Red Team"
        BLUE -> "Blue Team"
    }

    companion object
    {
        fun from(ordinal: Int) = entries[ordinal]
    }
}