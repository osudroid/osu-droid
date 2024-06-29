package com.reco1l.ibancho.data

enum class WinCondition
{
    SCORE_V1,
    ACCURACY,
    MAX_COMBO,
    SCORE_V2;

    companion object
    {
        fun from(ordinal: Int) = entries[ordinal]
    }
}