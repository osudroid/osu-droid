package com.reco1l.ibancho.data

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
        fun from(ordinal: Int) = values()[ordinal]
    }
}