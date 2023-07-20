package com.reco1l.api.ibancho.data

enum class TeamMode
{
    HEAD_TO_HEAD,
    TEAM_VS_TEAM;

    companion object
    {
        fun from(ordinal: Int) = values()[ordinal]
    }
}