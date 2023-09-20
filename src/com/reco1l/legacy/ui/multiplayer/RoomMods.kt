package com.reco1l.legacy.ui.multiplayer

import com.reco1l.legacy.data.modsToReadable
import com.reco1l.legacy.data.modsToString
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod.*
import ru.nsu.ccfit.zuev.osu.menu.ModMenu.getInstance as getModMenu
import java.util.*

data class RoomMods(

    var set: EnumSet<GameMod>,

    var speedMultiplier: Float,

    var flFollowDelay: Float,

    var forceAR: Float?

)
{

    override fun toString(): String
    {
        return if (Multiplayer.room?.isFreeMods == true)
        {
            buildString {

                append("Free mods")

                if (MOD_DOUBLETIME in set || MOD_NIGHTCORE in set)
                    append(", DT")

                if (MOD_HALFTIME in set)
                    append(", HT")

                if (speedMultiplier != 1f)
                    append(", ${speedMultiplier}x")

                if (flFollowDelay != getModMenu().defaultFLFollowDelay)
                    append(", ${flFollowDelay * 1000}ms FL delay")

                if (forceAR != null)
                    append(", AR $forceAR")
            }
        }
        else modsToReadable(modsToString(set))
    }

    override fun equals(other: Any?): Boolean
    {
        if (other === this)
            return true

        if (other !is RoomMods)
            return false

        val sameMods = Multiplayer.room?.isFreeMods == true

                // DoubleTime and NightCore, comparing them as one.
                && (MOD_DOUBLETIME in set || MOD_NIGHTCORE in set) == (MOD_DOUBLETIME in other.set || MOD_NIGHTCORE in other.set)
                // HalfTime
                && (MOD_HALFTIME in set == MOD_HALFTIME in other.set)
                // ScoreV2
                && (MOD_SCOREV2 in set == MOD_SCOREV2 in other.set)

                || set == other.set

        return sameMods
                && speedMultiplier == other.speedMultiplier
                && flFollowDelay == other.flFollowDelay
                && forceAR == other.forceAR
    }

    // Auto-generated this will help to check instance equality aka ===
    override fun hashCode(): Int
    {
        var result = set.hashCode()
        result = 31 * result + speedMultiplier.hashCode()
        result = 31 * result + flFollowDelay.hashCode()
        result = 31 * result + (forceAR?.hashCode() ?: 0)
        return result
    }
}